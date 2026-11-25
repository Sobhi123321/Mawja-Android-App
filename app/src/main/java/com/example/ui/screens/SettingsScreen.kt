package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ConnectionMode
import com.example.data.model.GatewayConfig
import com.example.data.model.ThemeMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.viewmodel.RouterUiState

@Composable
fun SettingsScreen(
    state: RouterUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectConnectionMode: (ConnectionMode) -> Unit,
    onSaveAndTestGateway: (GatewayConfig) -> Unit,
    onToggleIntruderAlerts: (Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val context = LocalContext.current
    val prefs = state.preferences

    var gatewayIp by remember(prefs.gatewayConfig.gatewayIp) { mutableStateOf(prefs.gatewayConfig.gatewayIp) }
    var routerBrand by remember(prefs.gatewayConfig.routerBrand) { mutableStateOf(prefs.gatewayConfig.routerBrand) }
    var adminUser by remember(prefs.gatewayConfig.adminUsername) { mutableStateOf(prefs.gatewayConfig.adminUsername) }
    var adminPass by remember(prefs.gatewayConfig.adminPassword) { mutableStateOf(prefs.gatewayConfig.adminPassword) }
    var useHttps by remember(prefs.gatewayConfig.useHttps) { mutableStateOf(prefs.gatewayConfig.useHttps) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_header") {
            Text(
                text = s.settingsHeaderTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Language & Appearance Card
        item(key = "appearance_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = s.appearanceSection,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Instant Language Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(s.languageLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = prefs.language == AppLanguage.AR,
                                onClick = { onSelectLanguage(AppLanguage.AR) },
                                label = { Text("العربية") },
                                modifier = Modifier.testTag("settings_lang_ar")
                            )
                            FilterChip(
                                selected = prefs.language == AppLanguage.EN,
                                onClick = { onSelectLanguage(AppLanguage.EN) },
                                label = { Text("English") },
                                modifier = Modifier.testTag("settings_lang_en")
                            )
                        }
                    }

                    // Theme Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = s.themeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(ThemeMode.AUTO, s.themeAuto, Icons.Rounded.SettingsBrightness),
                                Triple(ThemeMode.LIGHT, s.themeLight, Icons.Rounded.LightMode),
                                Triple(ThemeMode.DARK, s.themeDark, Icons.Rounded.DarkMode)
                            ).forEach { (mode, label, icon) ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSelectTheme(mode) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_theme_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connection Mode Card (Real LAN vs Demo)
        item(key = "connection_mode_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = s.connectionSection,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ModeOptionRow(
                        icon = Icons.Rounded.Router,
                        title = s.modeMikrotikTitle,
                        subtitle = s.modeMikrotikSubtitle,
                        selected = prefs.connectionMode == ConnectionMode.MIKROTIK,
                        onClick = { onSelectConnectionMode(ConnectionMode.MIKROTIK) },
                        modifier = Modifier.testTag("mode_option_mikrotik")
                    )

                    ModeOptionRow(
                        icon = Icons.Rounded.WifiFind,
                        title = s.modeLocalTitle,
                        subtitle = s.modeLocalSubtitle,
                        selected = prefs.connectionMode == ConnectionMode.LOCAL_LAN,
                        onClick = { onSelectConnectionMode(ConnectionMode.LOCAL_LAN) },
                        modifier = Modifier.testTag("mode_option_local")
                    )

                    ModeOptionRow(
                        icon = Icons.Rounded.Science,
                        title = s.modeDemoTitle,
                        subtitle = s.modeDemoSubtitle,
                        selected = prefs.connectionMode == ConnectionMode.DEMO,
                        onClick = { onSelectConnectionMode(ConnectionMode.DEMO) },
                        modifier = Modifier.testTag("mode_option_demo")
                    )
                }
            }
        }

        // Router Gateway Configuration
        item(key = "gateway_config_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = s.routerGatewaySection,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = gatewayIp,
                        onValueChange = { gatewayIp = it },
                        label = { Text(s.gatewayIpField) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gateway_ip_input")
                    )

                    OutlinedTextField(
                        value = routerBrand,
                        onValueChange = { routerBrand = it },
                        label = { Text(s.routerBrandField) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = adminUser,
                            onValueChange = { adminUser = it },
                            label = { Text(s.adminUserField) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = adminPass,
                            onValueChange = { adminPass = it },
                            label = { Text(s.adminPassField) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (prefs.connectionMode == ConnectionMode.MIKROTIK) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.useHttpsLabel, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = s.mtHttpsHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useHttps,
                                onCheckedChange = { useHttps = it },
                                modifier = Modifier.testTag("router_https_switch")
                            )
                        }
                    }

                    // Test result pill
                    state.gatewayTestResult?.let { ok ->
                        Surface(
                            color = if (ok) mawjaColors.healthyGreenContainer else mawjaColors.blockedRedContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (ok) mawjaColors.healthyGreen else mawjaColors.blockedRed
                                )
                                Text(
                                    text = state.gatewayTestDetail ?: if (ok) s.gatewayTestSuccess else s.gatewayTestFail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveAndTestGateway(
                                    GatewayConfig(
                                        gatewayIp = gatewayIp,
                                        routerBrand = routerBrand,
                                        adminUsername = adminUser,
                                        adminPassword = adminPass,
                                        useHttps = useHttps
                                    )
                                )
                            },
                            enabled = !state.isTestingGateway,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_gateway_button")
                        ) {
                            if (state.isTestingGateway) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(s.testGatewayBtn, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val cleanIp = gatewayIp.trim().ifEmpty { "192.168.1.1" }
                                val url = if (cleanIp.startsWith("http")) cleanIp else "http://$cleanIp"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_router_portal_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.openRouterAdminBtn, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Text(
                        text = if (prefs.connectionMode == ConnectionMode.MIKROTIK) s.mtPolicyNote else s.localPolicyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Security Notifications Card
        item(key = "security_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.intruderAlertsToggle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.intruderAlertsDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.intruderAlertsEnabled,
                        onCheckedChange = onToggleIntruderAlerts,
                        modifier = Modifier.testTag("intruder_alerts_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else LocalMawjaColors.current.cardBorder
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
