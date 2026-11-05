package app.islamiccalendar
import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * In-app updates via Google Play. Works only for installs from Play with a higher versionCode published.
 * Normal releases use a flexible update (background download, then a "Restart" prompt). A release marked with
 * update priority >= [URGENT_PRIORITY] in Play Console uses a blocking immediate update instead.
 */
object AppUpdate {
    private const val URGENT_PRIORITY = 4
    /** Do not prompt for a flexible update on the day it is published. */
    private const val MIN_STALENESS_DAYS = 1

    /** A flexible update has finished downloading and needs a restart to install. */
    var readyToInstall by mutableStateOf(false); private set

    private var mgr: AppUpdateManager? = null
    private val listener = InstallStateUpdatedListener { if (it.installStatus() == InstallStatus.DOWNLOADED) readyToInstall = true }

    fun init(a: Activity) {
        if (mgr != null) return
        mgr = AppUpdateManagerFactory.create(a.applicationContext).also { it.registerListener(listener) }
    }

    /** Call on every resume: starts an update, resumes an interrupted immediate one, or flags a finished download. */
    fun check(a: Activity) {
        val m = mgr ?: return
        m.appUpdateInfo.addOnSuccessListener { i ->
            if (i.installStatus() == InstallStatus.DOWNLOADED) { readyToInstall = true; return@addOnSuccessListener }
            val urgent = i.updatePriority() >= URGENT_PRIORITY && i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            when (i.updateAvailability()) {
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> if (urgent) start(m, i, a, AppUpdateType.IMMEDIATE)
                UpdateAvailability.UPDATE_AVAILABLE -> when {
                    urgent -> start(m, i, a, AppUpdateType.IMMEDIATE)
                    i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && (i.clientVersionStalenessDays() ?: 0) >= MIN_STALENESS_DAYS ->
                        start(m, i, a, AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    private fun start(m: AppUpdateManager, i: AppUpdateInfo, a: Activity, type: Int) {
        runCatching { m.startUpdateFlow(i, a, AppUpdateOptions.newBuilder(type).build()) }
    }

    fun install() { mgr?.completeUpdate() }
    fun dismiss() { readyToInstall = false }

    fun release() { mgr?.unregisterListener(listener); mgr = null }
}

/** Shown once a flexible update has downloaded. The user can restart now or keep using the app. */
@Composable fun UpdateReadyDialog() {
    if (!AppUpdate.readyToInstall) return
    AlertDialog(
        onDismissRequest = { AppUpdate.dismiss() },
        title = { Text(t("update_ready")) },
        confirmButton = { Button({ AppUpdate.install() }) { Text(t("update_restart")) } },
        dismissButton = { TextButton({ AppUpdate.dismiss() }) { Text(t("close")) } }
    )
}
