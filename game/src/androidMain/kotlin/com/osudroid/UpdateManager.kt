package com.osudroid

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.call.body
import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.reco1l.verktex.settings.Settings
import com.osudroid.settings.OsuSettings
import com.osudroid.settings.GameFolders
import com.reco1l.verktex.ui.toast.Toasts
import com.osudroid.android.AndroidGame
import android.content.*
import android.net.*
import android.util.*
import androidx.core.content.*
import com.osudroid.resources.Res
import com.osudroid.utils.*
import kotlinx.coroutines.CancellationException
import okio.Path.Companion.toOkioPath
import com.reco1l.osu.ui.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import com.osudroid.android.AppBuild
import java.io.*

object UpdateManager
{

    /**
     * The latest version of the game, as the server sends it.
     */
    @Serializable
    private class UpdateInfo(@SerialName("version_code") val versionCode: Long, val link: String)


    private val apksDirectory = File(GameFolders.cachePath, "updates").apply(File::mkdirs)


    private var progressDialog: ProgressDialog? = null


    @JvmStatic
    fun onActivityStart() = mainThread {

        val activity = AndroidGame.mainActivity!!

        // -1 indicates the first run of the game.
        val version = Settings[OsuSettings.Version]

        // Ignoring debug because otherwise every compiled build will show the dialog.
        if (!AppBuild.isDebug && version != -1L && version < activity.versionCode) {

            MessageDialog()
                .setTitle(Res.string.update_info_updated.toString())
                .setMessage("Game was updated to a newer version.\nDo you want to see the changelog?")
                .addButton("Yes") {
                    it.dismiss()

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://osudroid.moe/changelog/latest"))
                    AndroidGame.mainActivity!!.startActivity(intent)
                }
                .addButton("No", clickListener = MessageDialog::dismiss)
                .show()
        }

        Settings[OsuSettings.Version] = activity.versionCode
        checkNewUpdates(true)
    }

    /**
     * Check for new game updates.
     *
     * @param silently If `true`, no prompts will be shown unless there's new updates.
     */
    @JvmStatic
    fun checkNewUpdates(silently: Boolean) {

        if (!silently) {
            Toasts.show(Res.string.update_info_checking)
        }

        async {
            // Cleaning up old updates.
            apksDirectory.listFiles()?.forEach { it.delete() }

            try {
                val response = httpClient.get(OnlineManager.updateEndpoint) {
                    parameter("applicationId", AppBuild.applicationId)
                    parameter("buildType", AppBuild.buildType)
                }.body<UpdateInfo>()

                val newVersion = response.versionCode
                val link = response.link

                if (newVersion <= AndroidGame.mainActivity!!.versionCode) {
                    if (!silently) {
                        Toasts.show(Res.string.update_info_latest)
                    }
                    return@async
                }

                MessageDialog()
                    .setTitle("New update available!")
                    .setMessage(Res.string.update_dialog_message.toString())
                    .addButton(Res.string.update_dialog_button_update.toString()) { dialog ->
                        dialog.dismiss()

                        progressDialog = ProgressDialog().apply {
                            indeterminate = true
                            setAllowDismiss(false)
                            max = 100
                            title = "Downloading update"
                            message = Res.string.update_info_downloading.format(0)
                            show()
                        }

                        async {
                            downloadUpdate(link, File(apksDirectory, "${newVersion}.apk"))
                        }
                    }
                    .addButton("Update later") { it.dismiss() }
                    .show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates.", e)

                if (!silently) {
                    Toasts.show(Res.string.update_info_check_failed)
                }
            }
        }
    }


    private suspend fun downloadUpdate(link: String, file: File) {
        try {
            httpClient.downloadTo(link, file.toOkioPath(), onProgress = { download ->
                val progress = download.percentage.toInt()

                mainThread {
                    progressDialog?.indeterminate = progress < 0
                    progressDialog?.progress = progress.coerceAtLeast(0)
                    progressDialog?.setMessage(Res.string.update_info_downloading.format(progress.coerceAtLeast(0)).toString())
                }
            })
        } catch (e: CancellationException) {
            Toasts.show(Res.string.update_info_download_canceled)
            mainThread { progressDialog?.dismiss() }
            throw e
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to download update.", e)
            Toasts.show(Res.string.update_info_download_failed)
            mainThread { progressDialog?.dismiss() }
            return
        }

        mainThread { progressDialog?.dismiss() }

        val activity = AndroidGame.mainActivity!!
        val uri = FileProvider.getUriForFile(activity, "${AppBuild.applicationId}.fileProvider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activity.startActivity(intent)
    }
}
