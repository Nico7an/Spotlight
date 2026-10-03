package fr.nico7an.spotlight.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Liste des apps lançables et cache de leurs icônes. Le processus reste vivant grâce au
 * service d'accessibilité, ce qui permet d'ouvrir la recherche instantanément.
 */
class AppRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()
    private val iconSizePx = (ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt()
    private val icons = LruCache<String, ImageBitmap>(400)
    private var watchingPackages = false

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    fun refreshAsync() {
        scope.launch { refresh() }
    }

    suspend fun refresh() {
        val list = refreshMutex.withLock {
            withContext(Dispatchers.IO) { queryLauncherApps() }.also { _apps.value = it }
        }
        withContext(Dispatchers.IO) {
            list.forEach { if (icons.get(it.key) == null) loadIcon(it) }
        }
    }

    fun cachedIcon(app: AppEntry): ImageBitmap? = icons.get(app.key)

    suspend fun icon(app: AppEntry): ImageBitmap? =
        icons.get(app.key) ?: withContext(Dispatchers.IO) { loadIcon(app) }

    /** Rafraîchit la liste quand une app est installée, mise à jour ou supprimée. */
    fun watchPackageChanges() {
        if (watchingPackages) return
        watchingPackages = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.data?.schemeSpecificPart?.let { pkg ->
                    icons.snapshot().keys.filter { it.startsWith("$pkg/") }.forEach(icons::remove)
                }
                refreshAsync()
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun queryLauncherApps(): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return infos
            .map { info ->
                AppEntry(
                    label = info.loadLabel(pm).toString().trim(),
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name,
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.normLabel }
    }

    private fun loadIcon(app: AppEntry): ImageBitmap? = runCatching {
        val drawable = context.packageManager.getActivityIcon(ComponentName(app.packageName, app.className))
        val bitmap = drawable.toBitmap(iconSizePx, iconSizePx)
        bitmap.prepareToDraw()
        bitmap.asImageBitmap().also { icons.put(app.key, it) }
    }.getOrNull()

    companion object {
        private const val ICON_SIZE_DP = 48

        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
    }
}
