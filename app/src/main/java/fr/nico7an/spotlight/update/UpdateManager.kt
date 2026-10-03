package fr.nico7an.spotlight.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import fr.nico7an.spotlight.BuildConfig
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.ui.search.SearchActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Release(val version: String, val apkUrl: String, val apkSize: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Installing(val release: Release) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Mises à jour depuis les releases GitHub du projet : vérification, téléchargement de l'APK
 * et installation via [PackageInstaller]. Une fois que Spotlight a installé lui-même une
 * version, Android 12+ l'autorise à se mettre à jour sans confirmation.
 */
class UpdateManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var autoJob: Job? = null
    private var pending: Release? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val busy: Boolean
        get() = _state.value is UpdateState.Downloading || _state.value is UpdateState.Installing

    fun check() {
        scope.launch { mutex.withLock { checkNow() } }
    }

    /** Télécharge et installe la dernière version, avec confirmation si Android l'exige. */
    fun install() {
        scope.launch {
            mutex.withLock {
                val release = (_state.value as? UpdateState.Available)?.release ?: checkNow() ?: return@withLock
                downloadAndInstall(release, interactive = true)
            }
        }
    }

    /** Vérifie régulièrement et installe en arrière-plan si l'option est activée. */
    fun startAutoUpdates() {
        if (autoJob?.isActive == true) return
        autoJob = scope.launch {
            delay(AUTO_FIRST_DELAY_MS)
            while (isActive) {
                if (SettingsStore.get(context).autoUpdate) {
                    mutex.withLock {
                        val release = checkNow()
                        val canInstall = context.packageManager.canRequestPackageInstalls()
                        if (release != null && canInstall && !SearchActivity.isShowing) {
                            downloadAndInstall(release, interactive = false)
                        }
                    }
                }
                delay(AUTO_INTERVAL_MS)
            }
        }
    }

    private fun checkNow(): Release? {
        if (busy) return null
        _state.value = UpdateState.Checking
        return try {
            val release = fetchLatest()
            if (release != null && Versions.compare(release.version, BuildConfig.VERSION_NAME) > 0) {
                _state.value = UpdateState.Available(release)
                release
            } else {
                _state.value = UpdateState.UpToDate
                null
            }
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Vérification impossible : ${e.message ?: e.javaClass.simpleName}")
            null
        }
    }

    private fun fetchLatest(): Release? {
        val connection = open(LATEST_RELEASE_URL).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub a répondu ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.getString("name").endsWith(".apk") }
            val asset = apks.firstOrNull { "arm64" in it.getString("name") } ?: apks.firstOrNull() ?: return null
            return Release(
                version = json.getString("tag_name").removePrefix("v"),
                apkUrl = asset.getString("browser_download_url"),
                apkSize = asset.optLong("size"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadAndInstall(release: Release, interactive: Boolean) {
        try {
            val apk = download(release)
            _state.value = UpdateState.Installing(release)
            pending = release
            commitSession(apk, interactive)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Mise à jour impossible : ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun download(release: Release): File {
        val dir = File(context.cacheDir, "updates")
        val file = File(dir, "Spotlight-${release.version}.apk")
        // APK déjà téléchargé (ex. installation en attente de confirmation) : on le réutilise.
        if (file.exists() && release.apkSize > 0 && file.length() == release.apkSize) return file

        dir.deleteRecursively()
        dir.mkdirs()
        _state.value = UpdateState.Downloading(release, 0f)
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("téléchargement refusé (${connection.responseCode})")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) _state.value = UpdateState.Downloading(release, done.toFloat() / total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return file
    }

    private fun commitSession(apk: File, interactive: Boolean) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val callback = Intent(context, InstallResultReceiver::class.java)
                .putExtra(InstallResultReceiver.EXTRA_INTERACTIVE, interactive)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, sessionId, callback, flags).intentSender)
        }
    }

    /** Android demande une confirmation : l'utilisateur installera depuis les réglages. */
    internal fun onConfirmationNeeded() {
        _state.value = pending?.let { UpdateState.Available(it) } ?: UpdateState.Idle
    }

    internal fun onInstallFailed(message: String?) {
        _state.value = UpdateState.Failed("Installation échouée : ${message ?: "erreur inconnue"}")
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Spotlight-Android/${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Nico7an/Spotlight/releases/latest"
        private const val AUTO_FIRST_DELAY_MS = 2 * 60 * 1000L
        private const val AUTO_INTERVAL_MS = 6 * 60 * 60 * 1000L

        @Volatile
        private var instance: UpdateManager? = null

        fun get(context: Context): UpdateManager =
            instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
    }
}
