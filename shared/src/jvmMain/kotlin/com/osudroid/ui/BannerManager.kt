package com.osudroid.ui

import com.osudroid.online.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import com.osudroid.settings.GameFolders
import com.reco1l.verktex.Platform
import com.osudroid.utils.*

import com.reco1l.verktex.Logger
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import okio.Path.Companion.toPath

object BannerManager {

    /**
     * A banner, as the server sends it.
     */
    @Serializable
    private class BannerResponse(
        @SerialName("Url") val url: String,
        @SerialName("ImageLink") val imageLink: String,
    )

    private const val BANNER_DURATION = 10f // Seconds


    suspend fun loadBannerSprite(): BannerSprite? {

        val banners = mutableListOf<Banner>()

        try {

            val bannersFolder = GameFolders.cachePath.toPath() / "banners"
            Platform.fileSystem.createDirectories(bannersFolder)

            val responses = httpClient.get(OnlineManager.endpoint + "game/banner.php").body<List<BannerResponse>>()

            for ((i, banner) in responses.withIndex()) {

                try {
                    val bannerFile = bannersFolder / "${i}.png"
                    val bytes = httpClient.get(banner.imageLink).body<ByteArray>()
                    Platform.fileSystem.write(bannerFile) { write(bytes) }

                    banners.add(Banner(banner.url, Textures.load("banner@${i}", bannerFile.toString(), isInternal = false)))

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("BannerManager", "Failed to get banner at index ${i}.", e)
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("BannerManager", "Failed to get banners from server.", e)
        }

        if (banners.isEmpty()) {
            return null
        }

        return BannerSprite(banners)
    }


    data class Banner(
        val url: String,
        val image: GraphicsTexture.Region?,
    )

    class BannerSprite(private val banners: List<Banner>) : UISprite() {


        private var currentBannerIndex = 0

        private var elapsedTimeSinceLastChange = 0f


        init {
            textureRegion = banners[currentBannerIndex].image
            alpha = 0f
        }


        override fun onAttached() {
            super.onAttached()
            fadeIn(0.75f)
        }


        override fun onUpdate(clock: FrameClock) {
            if (banners.size > 1) {

                if (elapsedTimeSinceLastChange > BANNER_DURATION) {
                    elapsedTimeSinceLastChange %= BANNER_DURATION

                    currentBannerIndex++
                    currentBannerIndex %= banners.size

                    val banner = banners[currentBannerIndex]

                    fadeOut(0.5f).after {
                        textureRegion = banner.image
                        fadeIn(0.5f)
                    }
                }
                elapsedTimeSinceLastChange += clock.elapsedFrameTime
            }
            super.onUpdate(clock)
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            if (event !is PointerEvent || !containsScenePoint(event.x, event.y)) {
                return false
            }

            elapsedTimeSinceLastChange = 0f

            if (event.action == PointerEvent.Action.Down) {
                clearTransforms()
                scaleTo(0.95f, 0.1f)
            }

            if (event.action == PointerEvent.Action.Up || event.action == PointerEvent.Action.Cancel) {
                clearTransforms()
                scaleTo(1f, 0.1f)

                val banner = banners[currentBannerIndex]

                if (event.action == PointerEvent.Action.Up && banner.url.isNotBlank()) {
                    Platform.openUrl(banner.url)
                }
            }

            return true
        }
    }

}