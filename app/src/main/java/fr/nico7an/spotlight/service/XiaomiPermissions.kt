package fr.nico7an.spotlight.service

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Autorisations propriétaires MIUI / HyperOS, refusées par défaut pour les apps installées
 * hors store :
 * - « Afficher des fenêtres pop-up en arrière-plan » : sans elle, la recherche ne peut pas s'ouvrir ;
 * - « Démarrage automatique » : sans elle, le système ne relance pas le service une fois le
 *   processus tué (typiquement dès qu'on quitte l'app).
 */
object XiaomiPermissions {

    private const val OP_AUTO_START = 10008
    private const val OP_BACKGROUND_START_ACTIVITY = 10021
    private const val SECURITY_CENTER = "com.miui.securitycenter"

    val isXiaomi: Boolean
        get() = listOf("xiaomi", "redmi", "poco").any {
            Build.MANUFACTURER.equals(it, ignoreCase = true) || Build.BRAND.equals(it, ignoreCase = true)
        }

    /** `true`/`false` si l'état est lisible, `null` sinon (API cachée indisponible). */
    fun canStartFromBackground(context: Context): Boolean? = checkOp(context, OP_BACKGROUND_START_ACTIVITY)

    fun canAutoStart(context: Context): Boolean? = checkOp(context, OP_AUTO_START)

    fun openPermissionEditor(context: Context) {
        val editor = Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName)
        context.startFirst(
            Intent(editor).setClassName(SECURITY_CENTER, "com.miui.permcenter.permissions.PermissionsEditorActivity"),
            Intent(editor).setPackage(SECURITY_CENTER),
            appDetails(context),
        )
    }

    fun openAutoStart(context: Context) {
        context.startFirst(
            Intent().setComponent(ComponentName(SECURITY_CENTER, "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            appDetails(context),
        )
    }

    private fun checkOp(context: Context, op: Int): Boolean? = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        val mode = method.invoke(ops, op, Process.myUid(), context.packageName) as Int
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrNull()

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    private fun Context.startFirst(vararg intents: Intent) {
        for (intent in intents) {
            if (runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }
}
