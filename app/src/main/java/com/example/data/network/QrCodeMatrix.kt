package com.example.data.network

/**
 * Pure Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, ECC Level L, Versions 1..6)
 * Generates genuinely scannable QR matrices for WIFI:T:WPA;S:<ssid>;P:<pass>;; strings.
 */
object QrCodeMatrix {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalCodewords: Int,
        val dataCodewords: Int,
        val eccPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray
    )

    // Error Correction Level L specifications for Versions 1 to 6
    private val VERSION_SPECS = listOf(
        VersionSpec(1, 21, 26, 19, 7, 1, intArrayOf()),
        VersionSpec(2, 25, 44, 34, 10, 1, intArrayOf(6, 18)),
        VersionSpec(3, 29, 70, 55, 15, 1, intArrayOf(6, 22)),
        VersionSpec(4, 33, 100, 80, 20, 1, intArrayOf(6, 26)),
        VersionSpec(5, 37, 134, 108, 26, 1, intArrayOf(6, 30)),
        VersionSpec(6, 41, 172, 136, 18, 2, intArrayOf(6, 34))
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    fun encodeWifi(ssid: String, password: String): Array<BooleanArray> {
        val escSsid = ssid.replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:")
        val escPass = password.replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:")
        val payload = if (escPass.isEmpty()) {
            "WIFI:T:nopass;S:$escSsid;;"
        } else {
            "WIFI:T:WPA;S:$escSsid;P:$escPass;;"
        }
        return encode(payload)
    }

    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val payload = if (rawBytes.size > 132) rawBytes.copyOf(132) else rawBytes
        val spec = VERSION_SPECS.firstOrNull { (payload.size + 3) <= it.dataCodewords }
            ?: VERSION_SPECS.last()

        val dataBits = buildDataCodewords(payload, spec)
        val finalCodewords = interleaveWithEcc(dataBits, spec)

        val size = spec.size
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        placeFinderPattern(modules, isFunction, 0, 0)
        placeFinderPattern(modules, isFunction, size - 7, 0)
        placeFinderPattern(modules, isFunction, 0, size - 7)

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            modules[6][i] = dark
            isFunction[6][i] = true
            modules[i][6] = dark
            isFunction[i][6] = true
        }

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                val overlapsFinder = (cx < 9 && cy < 9) ||
                    (cx > size - 9 && cy < 9) ||
                    (cx < 9 && cy > size - 9)
                if (!overlapsFinder) {
                    placeAlignmentPattern(modules, isFunction, cx, cy)
                }
            }
        }

        // Dark module & reserve format info strips
        modules[size - 8][8] = true
        isFunction[size - 8][8] = true
        reserveFormatArea(isFunction, size)

        // Place data bits in zig-zag columns
        placeDataBits(modules, isFunction, finalCodewords, size)

        // Use Mask Pattern 0: (row + col) % 2 == 0, and write ECC-L + Mask 0 format bits
        val maskId = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c] && ((r + c) % 2 == 0)) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }
        writeFormatBits(modules, size, eccLevelBits = 0b01, maskId = maskId)
        return modules
    }

    private fun buildDataCodewords(payload: ByteArray, spec: VersionSpec): IntArray {
        val bits = ArrayList<Int>(spec.dataCodewords * 8)
        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        // Byte mode indicator (0100)
        pushBits(0b0100, 4)
        // Character count (8 bits for versions 1..9)
        pushBits(payload.size, 8)
        for (b in payload) {
            pushBits(b.toInt() and 0xFF, 8)
        }
        val capacityBits = spec.dataCodewords * 8
        val terminator = minOf(4, capacityBits - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        val codewords = IntArray(spec.dataCodewords)
        var idx = 0
        for (i in bits.indices step 8) {
            var v = 0
            for (j in 0 until 8) {
                v = (v shl 1) or bits[i + j]
            }
            if (idx < codewords.size) codewords[idx++] = v
        }
        var padToggle = true
        while (idx < codewords.size) {
            codewords[idx++] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }
        return codewords
    }

    private fun interleaveWithEcc(data: IntArray, spec: VersionSpec): IntArray {
        val blockSize = spec.dataCodewords / spec.numBlocks
        val dataBlocks = Array(spec.numBlocks) { b ->
            data.copyOfRange(b * blockSize, (b + 1) * blockSize)
        }
        val eccBlocks = Array(spec.numBlocks) { b ->
            computeReedSolomon(dataBlocks[b], spec.eccPerBlock)
        }
        val result = IntArray(spec.totalCodewords)
        var out = 0
        for (i in 0 until blockSize) {
            for (b in 0 until spec.numBlocks) {
                result[out++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.eccPerBlock) {
            for (b in 0 until spec.numBlocks) {
                result[out++] = eccBlocks[b][i]
            }
        }
        return result
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun computeReedSolomon(data: IntArray, eccCount: Int): IntArray {
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val next = IntArray(gen.size + 1)
            val root = GF_EXP[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
        }
        val rem = IntArray(eccCount)
        for (byteVal in data) {
            val factor = byteVal xor rem[0]
            System.arraycopy(rem, 1, rem, 0, eccCount - 1)
            rem[eccCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until eccCount) {
                    rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return rem
    }

    private fun placeFinderPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        originX: Int,
        originY: Int
    ) {
        val size = modules.size
        for (dy in -1..7) {
            for (dx in -1..7) {
                val x = originX + dx
                val y = originY + dy
                if (x in 0 until size && y in 0 until size) {
                    val isDark = dx in 0..6 && dy in 0..6 && (
                        dx == 0 || dx == 6 || dy == 0 || dy == 6 ||
                            (dx in 2..4 && dy in 2..4)
                        )
                    modules[y][x] = isDark
                    isFunction[y][x] = true
                }
            }
        }
    }

    private fun placeAlignmentPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        cx: Int,
        cy: Int
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val isDark = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1
                modules[cy + dy][cx + dx] = isDark
                isFunction[cy + dy][cx + dx] = true
            }
        }
    }

    private fun reserveFormatArea(isFunction: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        for (i in 0..7) {
            isFunction[8][size - 1 - i] = true
            isFunction[size - 1 - i][8] = true
        }
    }

    private fun placeDataBits(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: IntArray,
        size: Int
    ) {
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var upward = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right--
            for (vert in 0 until size) {
                val y = if (upward) size - 1 - vert else vert
                for (dx in 0..1) {
                    val x = right - dx
                    if (!isFunction[y][x]) {
                        var dark = false
                        if (bitIndex < totalBits) {
                            val cw = codewords[bitIndex ushr 3]
                            dark = ((cw ushr (7 - (bitIndex and 7))) and 1) != 0
                            bitIndex++
                        }
                        modules[y][x] = dark
                    }
                }
            }
            upward = !upward
            right -= 2
        }
    }

    private fun writeFormatBits(
        modules: Array<BooleanArray>,
        size: Int,
        eccLevelBits: Int,
        maskId: Int
    ) {
        val data = (eccLevelBits shl 3) or maskId
        var rem = data shl 10
        for (i in 4 downTo 0) {
            if (((rem ushr (i + 10)) and 1) != 0) {
                rem = rem xor (0x537 shl i)
            }
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        // First copy around top-left finder
        for (i in 0..5) modules[8][i] = ((bits ushr (14 - i)) and 1) != 0
        modules[8][7] = ((bits ushr 8) and 1) != 0
        modules[8][8] = ((bits ushr 7) and 1) != 0
        modules[7][8] = ((bits ushr 6) and 1) != 0
        for (i in 0..5) modules[5 - i][8] = ((bits ushr (5 - i)) and 1) != 0

        // Second copy along bottom-left and top-right
        for (i in 0..6) modules[size - 1 - i][8] = ((bits ushr (14 - i)) and 1) != 0
        for (i in 0..7) modules[8][size - 8 + i] = ((bits ushr (7 - i)) and 1) != 0
    }
}
