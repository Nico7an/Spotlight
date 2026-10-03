package fr.nico7an.spotlight.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.nico7an.spotlight.BuildConfig
import fr.nico7an.spotlight.R
import fr.nico7an.spotlight.core.Shortcut
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.service.XiaomiPermissions
import fr.nico7an.spotlight.ui.search.KeyCap
import fr.nico7an.spotlight.ui.search.SearchActivity
import fr.nico7an.spotlight.ui.theme.LocalSpotlightPalette

private data class Preset(val shortcut: Shortcut, val title: String, val subtitle: String)

private val PRESETS = listOf(
    Preset(Shortcut.META_TAP, "Touche Meta", "La touche ⊞ (4 carrés), pressée puis relâchée seule"),
    Preset(Shortcut.META_SPACE, "Meta + Espace", "Comme la recherche de Windows"),
    Preset(Shortcut.ALT_SPACE, "Alt + Espace", "Comme PowerToys Run / Raycast"),
    Preset(Shortcut.CTRL_SPACE, "Ctrl + Espace", "Attention : souvent utilisé pour changer de langue"),
)

@Composable
fun SettingsScreen(status: SystemStatus, settings: SettingsStore) {
    val context = LocalContext.current
    val palette = LocalSpotlightPalette.current

    var shortcut by remember { mutableStateOf(settings.shortcut) }
    var custom by remember { mutableStateOf(settings.customShortcut) }
    var blockSystem by remember { mutableStateOf(settings.blockSystemAction) }
    var suggestions by remember { mutableStateOf(settings.showSuggestions) }
    var blur by remember { mutableStateOf(settings.blurBackground) }
    var recording by remember { mutableStateOf(false) }

    fun applyShortcut(value: Shortcut) {
        shortcut = value
        settings.shortcut = value
    }

    Surface(color = palette.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .systemBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            ) {
                item { Header() }

                item { StatusGroup(status, shortcut) }

                if (status.isXiaomi || !status.batteryUnrestricted) {
                    item { BackgroundGroup(status) }
                }

                item {
                    SettingsGroup("Raccourci d'ouverture") {
                        PRESETS.forEach { preset ->
                            SettingsRow(
                                title = preset.title,
                                subtitle = preset.subtitle,
                                leading = { RadioButton(selected = shortcut == preset.shortcut, onClick = null) },
                                onClick = { applyShortcut(preset.shortcut) },
                            )
                            GroupDivider()
                        }
                        val customValue = custom
                        SettingsRow(
                            title = "Personnalisé",
                            subtitle = customValue?.label() ?: "N'importe quelle touche ou combinaison",
                            leading = { RadioButton(selected = shortcut !in Shortcut.PRESETS, onClick = null) },
                            trailing = {
                                TextButton(onClick = { recording = true }) {
                                    Text(if (customValue == null) "Enregistrer" else "Modifier")
                                }
                            },
                            onClick = { if (customValue == null) recording = true else applyShortcut(customValue) },
                        )
                    }
                }

                item {
                    SettingsGroup("Options") {
                        SwitchRow(
                            title = "Bloquer l'action système",
                            subtitle = "Empêche HyperOS de réagir à la touche déclencheur (raccourcis à touche seule)",
                            checked = blockSystem,
                            enabled = shortcut.isModifierTap,
                        ) {
                            blockSystem = it
                            settings.blockSystemAction = it
                        }
                        GroupDivider()
                        SwitchRow(
                            title = "Suggestions",
                            subtitle = "Afficher les apps les plus utilisées quand la recherche est vide",
                            checked = suggestions,
                        ) {
                            suggestions = it
                            settings.showSuggestions = it
                        }
                        GroupDivider()
                        SwitchRow(
                            title = "Flou d'arrière-plan",
                            subtitle = "Floute l'écran derrière la recherche (Android 12+)",
                            checked = blur,
                        ) {
                            blur = it
                            settings.blurBackground = it
                        }
                    }
                }

                item {
                    SettingsGroup {
                        SettingsRow(
                            title = "Ouvrir Spotlight",
                            subtitle = "Tester la fenêtre de recherche",
                            leading = { StatusBadge(Icons.Rounded.Search, palette.accent) },
                            trailing = { Chevron() },
                            onClick = { SearchActivity.open(context) },
                        )
                        GroupDivider()
                        SettingsRow(
                            title = "Paramètres d'accessibilité",
                            leading = { StatusBadge(Icons.Rounded.Settings, palette.secondaryText) },
                            trailing = { Chevron() },
                            onClick = { context.openAccessibilitySettings() },
                        )
                    }
                }

                item {
                    SettingsGroup("Au clavier") {
                        ShortcutHelpRow("Naviguer dans les résultats", "↑ ↓", "Tab")
                        GroupDivider()
                        ShortcutHelpRow("Ouvrir l'app sélectionnée", "Entrée")
                        GroupDivider()
                        ShortcutHelpRow("Effacer, puis fermer", "Échap")
                        GroupDivider()
                        ShortcutHelpRow("Ouvrir / fermer Spotlight", shortcut.label())
                    }
                }

                item {
                    Text(
                        text = "Spotlight ${BuildConfig.VERSION_NAME}",
                        color = palette.placeholder,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                    )
                }
            }
        }
    }

    if (recording) {
        RecordShortcutDialog(
            serviceEnabled = status.serviceRunning,
            onRecorded = {
                custom = it
                settings.customShortcut = it
                applyShortcut(it)
                recording = false
            },
            onDismiss = { recording = false },
        )
    }
}

@Composable
private fun StatusGroup(status: SystemStatus, shortcut: Shortcut) {
    val context = LocalContext.current
    val palette = LocalSpotlightPalette.current
    SettingsGroup {
        when {
            status.ready -> SettingsRow(
                title = "Spotlight est prêt",
                subtitle = "Appuyez sur ${shortcut.label()} pour rechercher une app",
                leading = { StatusBadge(Icons.Rounded.Check, palette.success) },
            )
            !status.serviceEnabled -> {
                SettingsRow(
                    title = "Service désactivé",
                    subtitle = "Activez « Spotlight – raccourci clavier » dans l'accessibilité pour détecter le raccourci.",
                    leading = { StatusBadge(Icons.Rounded.Warning, palette.warning) },
                    trailing = {
                        FilledTonalButton(onClick = { context.openAccessibilitySettings() }) { Text("Activer") }
                    },
                    onClick = { context.openAccessibilitySettings() },
                )
                GroupDivider()
                SettingsRow(
                    title = "Activation grisée ?",
                    subtitle = "Infos de l'app → ⋮ → « Autoriser les paramètres restreints », puis réessayez.",
                    trailing = { Chevron() },
                    onClick = { context.openAppDetails() },
                )
            }
            !status.serviceRunning -> SettingsRow(
                title = "Service arrêté par le système",
                subtitle = "Désactivez puis réactivez « Spotlight – raccourci clavier » dans l'accessibilité, " +
                    "après avoir autorisé le démarrage automatique ci-dessous.",
                leading = { StatusBadge(Icons.Rounded.Warning, palette.warning) },
                trailing = {
                    FilledTonalButton(onClick = { context.openAccessibilitySettings() }) { Text("Relancer") }
                },
                onClick = { context.openAccessibilitySettings() },
            )
            else -> SettingsRow(
                title = "Ouverture bloquée",
                subtitle = "HyperOS empêche Spotlight de s'afficher par-dessus les autres apps. " +
                    "Autorisez les pop-ups en arrière-plan ci-dessous.",
                leading = { StatusBadge(Icons.Rounded.Warning, palette.warning) },
            )
        }
    }
}

/** Autorisations qui empêchent HyperOS de bloquer la fenêtre ou de tuer le service. */
@Composable
private fun BackgroundGroup(status: SystemStatus) {
    val context = LocalContext.current
    SettingsGroup("Fonctionnement en arrière-plan") {
        if (status.isXiaomi) {
            PermissionRow(
                title = "Pop-ups en arrière-plan",
                granted = status.popupAllowed,
                grantedText = "Autorisé : la recherche peut s'ouvrir par-dessus les autres apps",
                missingText = "Autres autorisations → « Afficher des fenêtres pop-up en arrière-plan »",
                onClick = { XiaomiPermissions.openPermissionEditor(context) },
            )
            GroupDivider()
            PermissionRow(
                title = "Démarrage automatique",
                granted = status.autoStartAllowed,
                grantedText = "Autorisé : le service redémarre si le système l'arrête",
                missingText = "Indispensable pour que le raccourci marche une fois l'app fermée",
                onClick = { XiaomiPermissions.openAutoStart(context) },
            )
            GroupDivider()
        }
        PermissionRow(
            title = "Batterie sans restriction",
            granted = status.batteryUnrestricted,
            grantedText = "Spotlight n'est pas mis en veille par l'économiseur de batterie",
            missingText = "Évite que le service soit coupé en veille",
            onClick = { context.requestUnrestrictedBattery() },
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean?,
    grantedText: String,
    missingText: String,
    onClick: () -> Unit,
) {
    val palette = LocalSpotlightPalette.current
    SettingsRow(
        title = title,
        subtitle = if (granted == true) grantedText else missingText,
        leading = {
            if (granted == true) StatusBadge(Icons.Rounded.Check, palette.success)
            else StatusBadge(Icons.Rounded.Warning, palette.warning)
        },
        trailing = {
            if (granted == true) Chevron()
            else FilledTonalButton(onClick = onClick) { Text("Autoriser") }
        },
        onClick = onClick,
    )
}

@Composable
private fun Header() {
    val palette = LocalSpotlightPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(92.dp))
        Spacer(Modifier.height(18.dp))
        Text("Spotlight", fontSize = 38.sp, fontWeight = FontWeight.SemiBold, color = palette.primaryText)
        Spacer(Modifier.height(6.dp))
        Text("Lancez vos apps sans quitter le clavier", fontSize = 16.sp, color = palette.secondaryText)
    }
}

@Composable
private fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalSpotlightPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent,
                modifier = Modifier.padding(start = 22.dp, bottom = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(palette.card),
            content = content,
        )
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 22.dp),
        thickness = 1.dp,
        color = LocalSpotlightPalette.current.divider,
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val palette = LocalSpotlightPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = palette.primaryText)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = palette.secondaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        onClick = { onCheckedChange(!checked) },
    )
}

@Composable
private fun ShortcutHelpRow(title: String, vararg keys: String) {
    val palette = LocalSpotlightPalette.current
    SettingsRow(
        title = title,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.forEach { KeyCap(it, palette.secondaryText, palette.divider) }
            }
        },
    )
}

@Composable
private fun StatusBadge(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = LocalSpotlightPalette.current.placeholder,
    )
}

private fun Context.openAccessibilitySettings() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@SuppressLint("BatteryLife")
private fun Context.requestUnrestrictedBattery() {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(request) }.onFailure { openAppDetails() }
}

private fun Context.openAppDetails() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
