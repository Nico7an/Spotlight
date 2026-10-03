package fr.nico7an.spotlight.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * MIUI / HyperOS ajoutent une autorisation propriétaire « Afficher des fenêtres pop-up en
 * arrière-plan » (AppOp 10021), refusée par défaut pour les apps installées hors store.
 * Sans elle, le service ne peut pas ouvrir la fenêtre de recherche.
 */
object XiaomiPermissions {

    private const val OP_BACKGROUND_START_ACTIVITY = 10021

    val isXiaomi: Boolean
        get() = listOf("xiaomi", "redmi", "poco").any {
            Build.MANUFACTURER.equals(it, ignoreCase = true) || Build.BRAND.equals(it, ignoreCase = true)
        }

    /** `true`/`false` si l'état est lisible, `null` sinon (API cachée indisponible). */
    fun canStartFromBackground(context: Context): Boolean? = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        val mode = method.invoke(ops, OP_BACKGROUND_START_ACTIVITY, Process.myUid(), context.packageName) as Int
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrNull()

    fun openPermissionEditor(context: Context) {
        val editor = Intent("miui.intent.action.APP_PERM_EDITOR")
            .putExtra("extra_pkgname", context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val attempts = listOf(
            Intent(editor).setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"),
            Intent(editor).setPackage("com.miui.securitycenter"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        for (intent in attempts) {
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
}
