package com.osudroid

import android.content.Intent
import androidx.core.content.FileProvider
import com.osudroid.android.AndroidGame
import com.osudroid.android.AppBuild
import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.osudroid.resources.Res
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.toast.Toasts
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import ru.nsu.ccfit.zuev.osu.online.OnlineManager


object UpdateManager
{

    /**
     * The latest version of the game, as the server sends it.
     */
    @Serializable
    private class UpdateInfo(@SerialName("version_code") val versionCode: Long, val link: String)


    private val apksDirectory = File(GameFolders.cachePath, "updates").apply(File::mkdirs)


    @JvmStatic
    fun onActivityStart() {

        val activity = AndroidGame.mainActivity!!

        // -1 indicates the first run of the game.
        val version = Settings[OsuSettings.Version]

        // Ignoring debug because otherwise every compiled build will show the dialog.
        if (!AppBuild.isDebug && version != -1L && version < activity.versionCode) {
            updateThread {
                UIConfirmDialog().apply {
                    title = Res.string.update_info_updated
                    text = "Game was updated to a newer version.\nDo you want to see the changelog?"
                    detachOnHide = true
                    onConfirm = { Platform.openUrl("https://osudroid.moe/changelog/latest") }
                }.show()
            }
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

                updateThread {
                    UIMessageDialog().apply {
                        title = "New update available!"
                        text = Res.string.update_dialog_message
                        detachOnHide = true

                        addButton {
                            text = Res.string.update_dialog_button_update
                            variant = UIButton.Variant.Solid
                            onActionUp = {
                                hide()
                                async { downloadUpdate(link, File(apksDirectory, "${newVersion}.apk")) }
                            }
                        }

                        addButton {
                            text = "Update later"
                            onActionUp = { hide() }
                        }
                    }.show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("UpdateManager", "Failed to check for updates.", e)

                if (!silently) {
                    Toasts.show(Res.string.update_info_check_failed)
                }
            }
        }
    }


    private suspend fun downloadUpdate(link: String, file: File) {
        val progress = Toasts.showPersistent(
            text = Res.string.update_info_downloading.format(0),
            title = "Downloading update",
            isIndeterminate = true
        )

        try {
            httpClient.downloadTo(link, file.toOkioPath(), onProgress = { download ->
                val percentage = download.percentage.toInt()

                progress.isIndeterminate = percentage < 0
                progress.progress = percentage.coerceAtLeast(0) / 100f
                progress.text = Res.string.update_info_downloading.format(percentage.coerceAtLeast(0))
            })
        } catch (e: CancellationException) {
            progress.dismiss()
            Toasts.show(Res.string.update_info_download_canceled)
            throw e
        } catch (e: Exception) {
            Logger.e("UpdateManager", "Failed to download update.", e)
            progress.dismiss()
            Toasts.show(Res.string.update_info_download_failed)
            return
        }

        progress.dismiss()

        val activity = AndroidGame.mainActivity!!
        val uri = FileProvider.getUriForFile(activity, "${AppBuild.applicationId}.fileProvider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activity.startActivity(intent)
    }
}
