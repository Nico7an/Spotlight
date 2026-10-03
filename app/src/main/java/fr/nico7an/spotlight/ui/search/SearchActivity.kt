package fr.nico7an.spotlight.ui.search

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import fr.nico7an.spotlight.R
import fr.nico7an.spotlight.data.AppEntry
import fr.nico7an.spotlight.data.AppRepository
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.data.UsageStore
import fr.nico7an.spotlight.ui.theme.SpotlightTheme
import java.lang.ref.WeakReference

class SearchActivity : ComponentActivity() {

    /** Incrémenté à chaque réouverture pour repartir d'une recherche vide. */
    private var session by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = transparent, navigationBarStyle = transparent)
        super.onCreate(savedInstanceState)
        current = WeakReference(this)

        val settings = SettingsStore.get(this)
        setupWindow(settings.blurBackground)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.spotlight_enter, R.anim.spotlight_hold)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.spotlight_hold, R.anim.spotlight_exit)
        }

        val repository = AppRepository.get(this).also { it.refreshAsync() }
        val usage = UsageStore.get(this)

        val root = PreImeKeyLayout(this)
        root.addView(
            ComposeView(this).apply {
                setContent {
                    SpotlightTheme {
                        SearchScreen(
                            repository = repository,
                            usage = usage,
                            showSuggestions = settings.showSuggestions,
                            session = session,
                            onLaunchApp = ::launchApp,
                            onStoreSearch = ::searchStore,
                            onDismiss = ::finish,
                            registerKeyHandler = { root.onKey = it },
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        setContentView(root)
    }

    private fun setupWindow(blur: Boolean) {
        val canBlur = blur && Build.VERSION.SDK_INT >= 31 && windowManager.isCrossWindowBlurEnabled
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (canBlur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also {
                it.blurBehindRadius = (BLUR_RADIUS_DP * resources.displayMetrics.density).toInt()
            }
        }
        window.setDimAmount(if (canBlur) 0.2f else 0.4f)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        session++
    }

    override fun onStop() {
        super.onStop()
        // Comme Spotlight sur macOS : dès qu'on passe à autre chose, la fenêtre disparaît.
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (current?.get() === this) current = null
    }

    private fun launchApp(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
            UsageStore.get(this).record(app.key)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun searchStore(query: String) {
        val encoded = Uri.encode(query)
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(market)
        } catch (e: ActivityNotFoundException) {
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        finish()
    }

    companion object {
        private const val BLUR_RADIUS_DP = 24

        private var current: WeakReference<SearchActivity>? = null

        fun open(context: Context) {
            context.startActivity(
                Intent(context, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        val isShowing: Boolean
            get() = current?.get()?.let { !it.isFinishing && !it.isDestroyed } ?: false

        /** Ouvre la recherche, ou la ferme si elle est déjà affichée. */
        fun toggle(context: Context) {
            if (isShowing) current?.get()?.finish() else open(context)
        }
    }
}
