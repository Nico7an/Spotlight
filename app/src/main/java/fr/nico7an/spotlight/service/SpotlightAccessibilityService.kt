package fr.nico7an.spotlight.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ServiceCompat
import fr.nico7an.spotlight.R
import fr.nico7an.spotlight.core.KeyInput
import fr.nico7an.spotlight.core.ShortcutRecorder
import fr.nico7an.spotlight.core.TriggerDetector
import fr.nico7an.spotlight.data.AppRepository
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.ui.search.SearchActivity
import fr.nico7an.spotlight.update.UpdateManager
import fr.nico7an.spotlight.ui.settings.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Filtre les touches du clavier physique avant le système pour détecter le raccourci
 * d'ouverture. Seul le raccourci est consommé : toutes les autres touches passent.
 */
class SpotlightAccessibilityService : AccessibilityService() {

    private lateinit var settings: SettingsStore
    private val detector = TriggerDetector { SearchActivity.toggle(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsStore.get(this)
        _running.value = true
        keepAlive()
        AppRepository.get(this).refreshAsync()
        UpdateManager.get(this).startAutoUpdates()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _running.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _running.value = false
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        ShortcutRecorder.active?.let { return it.onKey(event) }
        if (!::settings.isInitialized) return false
        return detector.onKey(KeyInput.of(event), settings.shortcut, settings.blockSystemAction)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /**
     * Passe le service au premier plan pour que HyperOS ne tue pas le processus dès que l'app
     * quitte l'écran. La notification, de priorité minimale, n'est visible que si l'utilisateur
     * autorise les notifications de Spotlight.
     */
    private fun keepAlive() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Service Spotlight", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Maintient le raccourci clavier actif"
                setShowBadge(false)
            },
        )
        val openSettings = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Spotlight est actif")
            .setContentText("Raccourci : ${SettingsStore.get(this).shortcut.label()}")
            .setContentIntent(openSettings)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1

        private val _running = MutableStateFlow(false)

        /** Le service est réellement lié par le système (et pas seulement coché dans l'accessibilité). */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val me = ComponentName(context, SpotlightAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
