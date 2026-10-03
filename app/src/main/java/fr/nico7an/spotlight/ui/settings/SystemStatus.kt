package fr.nico7an.spotlight.ui.settings

import android.content.Context
import android.os.PowerManager
import fr.nico7an.spotlight.service.SpotlightAccessibilityService
import fr.nico7an.spotlight.service.XiaomiPermissions

/** Tout ce dont Spotlight a besoin côté système, relu à chaque retour sur l'écran de réglages. */
data class SystemStatus(
    val serviceEnabled: Boolean = false,
    val serviceRunning: Boolean = false,
    val isXiaomi: Boolean = false,
    /** `null` : état illisible sur cet appareil. */
    val popupAllowed: Boolean? = true,
    val autoStartAllowed: Boolean? = true,
    val batteryUnrestricted: Boolean = true,
) {
    val ready: Boolean
        get() = serviceEnabled && serviceRunning && popupAllowed != false

    companion object {
        fun read(context: Context): SystemStatus {
            val xiaomi = XiaomiPermissions.isXiaomi
            val power = context.getSystemService(PowerManager::class.java)
            return SystemStatus(
                serviceEnabled = SpotlightAccessibilityService.isEnabled(context),
                serviceRunning = SpotlightAccessibilityService.running.value,
                isXiaomi = xiaomi,
                popupAllowed = if (xiaomi) XiaomiPermissions.canStartFromBackground(context) else true,
                autoStartAllowed = if (xiaomi) XiaomiPermissions.canAutoStart(context) else true,
                batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}
