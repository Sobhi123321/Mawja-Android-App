package com.example.data.network

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.max
import kotlin.math.roundToInt

class MikroTikException(
    val kind: Kind,
    val httpCode: Int = 0,
    message: String
) : Exception(message) {
    enum class Kind { UNREACHABLE, AUTH, TLS, SERVER }
}

data class MikroTikInfo(
    val boardName: String,
    val version: String,
    val uptimeHours: Int
)

data class MikroTikLease(
    val mac: String,
    val ip: String,
    val hostName: String,
    val isOnline: Boolean
)

data class MikroTikLimit(
    val downMbps: Int,
    val upMbps: Int
)

/**
 * Minimal RouterOS v7 REST API client (https://help.mikrotik.com/docs/spaces/ROS/pages/47579162/REST+API).
 *
 * All calls are blocking and must be made off the main thread.
 *
 * - Devices: /ip/dhcp-server/lease + /ip/arp
 * - Blocking: address-list "mawja-blocked" + one /ip/firewall/raw drop rule (prerouting)
 * - Speed limits: /queue/simple named "mawja-<MAC>"
 * - Live traffic: byte counters of the WAN interface (/interface)
 */
class MikroTikClient(
    rawHost: String,
    private val username: String,
    private val password: String,
    private val useHttps: Boolean
) {
    private val hostPort: String = rawHost.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
    private val scheme: String = if (useHttps) "https" else "http"

    @Volatile
    private var wanInterface: String? = null

    private val http: OkHttpClient = buildHttp()

    private fun buildHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
        if (useHttps) {
            // RouterOS ships with a self-signed certificate, so it can never pass normal validation.
            // Validation is therefore skipped, and only for the one router address the user typed in.
            // The traffic is still encrypted, but the router's identity is not verified, so this
            // protects against passive sniffing on the Wi-Fi and not against an active attacker.
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            builder.sslSocketFactory(ctx.socketFactory, trustAll)
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
        return builder.build()
    }

    private fun call(
        method: String,
        segments: List<String>,
        query: Map<String, String> = emptyMap(),
        body: JSONObject? = null
    ): String {
        val root = "$scheme://$hostPort/rest".toHttpUrlOrNull()
            ?: throw MikroTikException(MikroTikException.Kind.UNREACHABLE, 0, "Invalid router address")
        val urlBuilder = root.newBuilder()
        segments.forEach { urlBuilder.addPathSegment(it) }
        query.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

        val payload = (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType())
        val rb = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", Credentials.basic(username, password))
        when (method) {
            "GET" -> rb.get()
            "PUT" -> rb.put(payload)
            "PATCH" -> rb.patch(payload)
            "DELETE" -> rb.delete()
            else -> rb.post(payload)
        }

        return try {
            http.newCall(rb.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val kind = if (resp.code == 401 || resp.code == 403) {
                        MikroTikException.Kind.AUTH
                    } else {
                        MikroTikException.Kind.SERVER
                    }
                    throw MikroTikException(kind, resp.code, extractDetail(text, resp.code))
                }
                text
            }
        } catch (e: MikroTikException) {
            throw e
        } catch (e: SSLException) {
            throw MikroTikException(MikroTikException.Kind.TLS, 0, e.message ?: "TLS error")
        } catch (e: IOException) {
            throw MikroTikException(MikroTikException.Kind.UNREACHABLE, 0, e.message ?: "Unreachable")
        }
    }

    private fun extractDetail(text: String, code: Int): String {
        val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
        return if (!detail.isNullOrBlank()) detail else "Router returned HTTP $code"
    }

    private fun getArray(segments: List<String>, query: Map<String, String> = emptyMap()): JSONArray {
        val text = call("GET", segments, query)
        return try {
            JSONArray(text)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    // ---------------------------------------------------------------- info

    fun fetchInfo(): MikroTikInfo {
        val text = call("GET", listOf("system", "resource"))
        val o = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw MikroTikException(MikroTikException.Kind.SERVER, 0, "Unexpected response from router")
        }
        return MikroTikInfo(
            boardName = o.optString("board-name", "").ifBlank { "RouterOS" },
            version = o.optString("version", "").substringBefore(" "),
            uptimeHours = parseUptimeHours(o.optString("uptime", ""))
        )
    }

    // ------------------------------------------------------------- devices

    fun fetchDevices(): List<MikroTikLease> {
        val leases = getArray(listOf("ip", "dhcp-server", "lease"))
        val arp = getArray(listOf("ip", "arp"))
        val byMac = LinkedHashMap<String, MikroTikLease>()

        for (o in leases.objects()) {
            val mac = normMac(o.optString("mac-address")) ?: continue
            val ip = o.optString("active-address").ifBlank { o.optString("address") }
            if (!looksLikeIpv4(ip)) continue
            val host = o.optString("host-name").ifBlank { o.optString("comment") }
            byMac[mac] = MikroTikLease(mac = mac, ip = ip, hostName = host, isOnline = false)
        }

        // ARP entries on the WAN side belong to the ISP / upstream router, not to the home network.
        val wan = wanInterface ?: runCatching { detectWanInterface() }.getOrNull()
        for (o in arp.objects()) {
            val mac = normMac(o.optString("mac-address")) ?: continue
            val ip = o.optString("address")
            if (wan != null && o.optString("interface") == wan && byMac[mac] == null) continue
            val online = o.optString("complete") != "false" &&
                o.optString("invalid") != "true" &&
                o.optString("disabled") != "true"
            val prev = byMac[mac]
            if (prev != null) {
                byMac[mac] = prev.copy(
                    ip = if (online && looksLikeIpv4(ip)) ip else prev.ip,
                    isOnline = online
                )
            } else if (looksLikeIpv4(ip)) {
                byMac[mac] = MikroTikLease(
                    mac = mac,
                    ip = ip,
                    hostName = o.optString("comment"),
                    isOnline = online
                )
            }
        }
        return byMac.values.toList()
    }

    // ------------------------------------------------------------ blocking

    fun fetchBlockedMacs(): Set<String> {
        val list = getArray(
            listOf("ip", "firewall", "address-list"),
            mapOf("list" to BLOCK_LIST)
        )
        return list.objects()
            .map { it.optString("comment") }
            .filter { it.startsWith(TAG_PREFIX) }
            .mapNotNull { normMac(it.removePrefix(TAG_PREFIX)) }
            .toSet()
    }

    fun setBlocked(mac: String, ip: String, blocked: Boolean) {
        val tag = "$TAG_PREFIX$mac"
        val entries = getArray(
            listOf("ip", "firewall", "address-list"),
            mapOf("list" to BLOCK_LIST)
        ).objects().filter { it.optString("comment") == tag }

        if (blocked) {
            if (!looksLikeIpv4(ip)) {
                throw MikroTikException(MikroTikException.Kind.SERVER, 0, "Device IP address is unknown")
            }
            ensureBlockRule()
            if (entries.isEmpty()) {
                call(
                    "PUT",
                    listOf("ip", "firewall", "address-list"),
                    body = JSONObject()
                        .put("list", BLOCK_LIST)
                        .put("address", ip)
                        .put("comment", tag)
                )
            } else {
                val first = entries.first()
                if (first.optString("address") != ip) {
                    call(
                        "PATCH",
                        listOf("ip", "firewall", "address-list", first.optString(".id")),
                        body = JSONObject().put("address", ip)
                    )
                }
            }
        } else {
            entries.forEach {
                call("DELETE", listOf("ip", "firewall", "address-list", it.optString(".id")))
            }
        }
    }

    /** One raw (pre-conntrack) drop rule for every address in the block list. Created once. */
    private fun ensureBlockRule() {
        val rules = getArray(
            listOf("ip", "firewall", "raw"),
            mapOf("comment" to RULE_COMMENT)
        ).objects()
        if (rules.none { it.optString("comment") == RULE_COMMENT }) {
            call(
                "PUT",
                listOf("ip", "firewall", "raw"),
                body = JSONObject()
                    .put("chain", "prerouting")
                    .put("src-address-list", BLOCK_LIST)
                    .put("action", "drop")
                    .put("comment", RULE_COMMENT)
            )
        }
    }

    // --------------------------------------------------------- speed limit

    fun fetchLimits(): Map<String, MikroTikLimit> {
        val out = HashMap<String, MikroTikLimit>()
        for (o in getArray(listOf("queue", "simple")).objects()) {
            val name = o.optString("name")
            if (!name.startsWith(QUEUE_PREFIX)) continue
            val mac = normMac(name.removePrefix(QUEUE_PREFIX)) ?: continue
            val parts = o.optString("max-limit").split("/")
            // RouterOS max-limit is "<upload>/<download>"
            out[mac] = MikroTikLimit(
                downMbps = rateToMbps(parts.getOrNull(1)),
                upMbps = rateToMbps(parts.getOrNull(0))
            )
        }
        return out
    }

    fun setLimits(mac: String, ip: String, downMbps: Int, upMbps: Int) {
        val name = "$QUEUE_PREFIX$mac"
        val ids = getArray(listOf("queue", "simple"), mapOf("name" to name))
            .objects()
            .filter { it.optString("name") == name }
            .map { it.optString(".id") }

        if (downMbps <= 0 && upMbps <= 0) {
            ids.forEach { call("DELETE", listOf("queue", "simple", it)) }
            return
        }
        if (!looksLikeIpv4(ip)) {
            throw MikroTikException(MikroTikException.Kind.SERVER, 0, "Device IP address is unknown")
        }
        val body = JSONObject()
            .put("target", "$ip/32")
            .put("max-limit", "${fmtRate(upMbps)}/${fmtRate(downMbps)}")
        if (ids.isEmpty()) {
            call(
                "PUT",
                listOf("queue", "simple"),
                body = body.put("name", name).put("comment", "Mawja app")
            )
        } else {
            call("PATCH", listOf("queue", "simple", ids.first()), body = body)
        }
    }

    // ------------------------------------------------------- live traffic

    /** Returns cumulative (rx, tx) bytes of the WAN interface. rx = download. */
    fun fetchWanCounters(): Pair<Long, Long> {
        val iface = wanInterface ?: detectWanInterface().also { wanInterface = it }
        val row = getArray(listOf("interface"), mapOf("name" to iface))
            .objects()
            .firstOrNull { it.optString("name") == iface }
        if (row == null) {
            wanInterface = null
            throw MikroTikException(MikroTikException.Kind.SERVER, 0, "WAN interface $iface not found")
        }
        return Pair(
            row.optString("rx-byte").toLongOrNull() ?: 0L,
            row.optString("tx-byte").toLongOrNull() ?: 0L
        )
    }

    private fun detectWanInterface(): String {
        val routes = getArray(listOf("ip", "route"), mapOf("dst-address" to "0.0.0.0/0")).objects()
        for (o in routes) {
            if (o.optString("active") != "true") continue
            val gw = o.optString("immediate-gw").ifBlank { o.optString("gateway") }
            val iface = if (gw.contains('%')) gw.substringAfter('%') else gw
            if (iface.isNotBlank() && iface.any { it.isLetter() }) return iface
        }
        return "ether1"
    }

    // ------------------------------------------------------------- helpers

    companion object {
        const val BLOCK_LIST = "mawja-blocked"
        const val RULE_COMMENT = "mawja-block"
        const val TAG_PREFIX = "mawja:"
        const val QUEUE_PREFIX = "mawja-"

        private val MAC_REGEX = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")
        private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

        fun normMac(raw: String?): String? {
            val m = raw?.trim()?.uppercase() ?: return null
            return if (MAC_REGEX.matches(m)) m else null
        }

        fun looksLikeIpv4(ip: String): Boolean = IPV4_REGEX.matches(ip.trim())

        fun parseUptimeHours(s: String): Int {
            var hours = 0L
            Regex("(\\d+)([wdhms])").findAll(s).forEach { m ->
                val n = m.groupValues[1].toLongOrNull() ?: 0L
                hours += when (m.groupValues[2]) {
                    "w" -> n * 168L
                    "d" -> n * 24L
                    "h" -> n
                    else -> 0L
                }
            }
            return hours.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        }

        fun rateToMbps(raw: String?): Int {
            val t = raw?.trim().orEmpty()
            if (t.isEmpty()) return 0
            val last = t.last()
            val multiplier = when (last.lowercaseChar()) {
                'k' -> 1_000.0
                'm' -> 1_000_000.0
                'g' -> 1_000_000_000.0
                else -> 1.0
            }
            val numberPart = if (last.isLetter()) t.dropLast(1) else t
            val bits = (numberPart.toDoubleOrNull() ?: 0.0) * multiplier
            if (bits <= 0.0) return 0
            return max(1, (bits / 1_000_000.0).roundToInt())
        }

        fun fmtRate(mbps: Int): String = if (mbps <= 0) "0" else "${mbps}M"
    }
}
