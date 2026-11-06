package com.osudroid

import com.reco1l.verktex.settings.Settings
import com.osudroid.settings.OsuSettings
import com.osudroid.settings.GameFolders
import com.reco1l.verktex.ui.toast.Toasts
import com.osudroid.android.AndroidGame
import com.osudroid.utils.getLong
import com.osudroid.utils.getString
import android.content.*
import android.net.*
import android.util.*
import androidx.core.content.*
import com.osudroid.resources.Res
import com.osudroid.utils.*
import com.osudroid.online.OsuHttp
import kotlinx.coroutines.CancellationException
import okio.Path.Companion.toOkioPath
import com.reco1l.osu.ui.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*

object UpdateManager
{

    private val apksDirectory = File(GameFolders.cachePath, "updates").apply(File::mkdirs)


    private var progressDialog: ProgressDialog? = null


    @JvmStatic
    fun onActivityStart() = mainThread {

        val activity = AndroidGame.mainActivity!!

        // -1 indicates the first run of the game.
        val version = Settings[OsuSettings.Version]

        // Ignoring debug because otherwise every compiled build will show the dialog.
        if (!BuildConfig.DEBUG && version != -1L && version < activity.versionCode) {

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
                val response = OsuHttp.getJsonObject(OnlineManager.updateEndpoint) {
                    url {
                        parameters.append("applicationId", BuildConfig.APPLICATION_ID)
                        parameters.append("buildType", BuildConfig.BUILD_TYPE)
                    }
                }

                val newVersion = response.getLong("version_code")
                val link = response.getString("link")

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
            OsuHttp.download(link, file.toOkioPath(), onProgress = { download ->
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
        val uri = FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.fileProvider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activity.startActivity(intent)
    }
}
