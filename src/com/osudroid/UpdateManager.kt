package com.osudroid

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
import com.reco1l.framework.net.*
import com.reco1l.osu.ui.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*

object UpdateManager: IFileRequestObserver
{

    private val apksDirectory = File(Config.getCachePath(), "updates").apply(File::mkdirs)


    private var progressDialog: ProgressDialog? = null


    @JvmStatic
    fun onActivityStart() = mainThread {

        val activity = AndroidGame.mainActivity!!

        // -1 indicates the first run of the game.
        val version = Config.getLong("version", -1)

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

        Config.setLong("version", activity.versionCode)
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
                JsonObjectRequest(OnlineManager.updateEndpoint).use { request ->
                    request.buildUrl {
                        addQueryParameter("applicationId", BuildConfig.APPLICATION_ID)
                        addQueryParameter("buildType", BuildConfig.BUILD_TYPE)
                    }

                    val response = request.execute().json

                    val newVersion = response.getLong("version_code")
                    val link = response.getString("link")

                    if (newVersion <= AndroidGame.mainActivity!!.versionCode) {
                        if (!silently) {
                            Toasts.show(Res.string.update_info_latest)
                        }
                        return@use
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
                                val file = File(apksDirectory, "${newVersion}.apk")
                                if (file.exists()) {
                                    file.delete()
                                }
                                file.createNewFile()

                                val fileRequest = FileRequest(file.path, link)
                                fileRequest.observer = this@UpdateManager
                                fileRequest.execute()
                            }
                        }
                        .addButton("Update later") { it.dismiss() }
                        .show()
                }

            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates.", e)

                if (!silently) {
                    Toasts.show(Res.string.update_info_check_failed)
                }
            }
        }
    }


    override fun onDownloadUpdate(request: FileRequest) = mainThread {
        val progress = request.progress.toInt()

        progressDialog?.indeterminate = false
        progressDialog?.progress = progress
        progressDialog?.setMessage(Res.string.update_info_downloading.format(progress).toString())
    }

    override fun onDownloadEnd(request: FileRequest) {
        mainThread { progressDialog?.dismiss() }

        val activity = AndroidGame.mainActivity!!
        val uri = FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.fileProvider", File(request.path))

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activity.startActivity(intent)
    }

    override fun onDownloadFail(request: FileRequest, exception: Exception) {
        Log.e("UpdateManager", "Failed to download update.", exception)
        Toasts.show(Res.string.update_info_download_failed)

        mainThread { progressDialog?.dismiss() }
        File(request.path).delete()
    }

    override fun onDownloadCancel(request: FileRequest) {
        Toasts.show(Res.string.update_info_download_canceled)

        mainThread { progressDialog?.dismiss() }
        File(request.path).delete()
    }
}