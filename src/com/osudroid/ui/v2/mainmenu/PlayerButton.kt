package com.osudroid.ui.v2.mainmenu

import com.osudroid.ui.v2.CachedTextures
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring

class PlayerButton : UIClickableContainer() {

    private val onlineManager = OnlineManager.getInstance()
    private val onlineScoring = OnlineScoring.getInstance()

    private var avatarUri = ""
    private var avatarLoaded = false

    private lateinit var nameText: UIText
    private lateinit var informationText: UIText
    private lateinit var avatarSprite: UISprite


    init {
        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            padding = Padding(9f.dip)
            spacing = 8f.dip
            style = Style { theme ->
                radius = theme.radius.xl
                backgroundColor = (theme.palette.accent * 0.1f).copy(alpha = 0.2f)
            }

            avatarSprite = sprite {
                width = 48f.dip
                height = 48f.dip
                style = Style { theme ->
                    backgroundColor = (theme.palette.accent * 0.1f).copy(alpha = 0.3f)
                    radius = theme.radius.xl
                }
                scaleType = UISprite.ScaleType.Crop
                textureRegion = SkinTextures["emptyavatar"]
            }

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = UILinearContainer.Orientation.Vertical

                nameText = text {
                    text = "Player"
                    style = Style { theme ->
                        color = theme.palette.accent
                    }
                }

                informationText = text {
                    text = "Loading..."
                    style = Style { theme ->
                        fontSize = theme.typography.smallCaption.size
                        color = theme.palette.accent * 0.8f
                    }
                }
            }
        }
    }

    override fun onUpdate(clock: FrameClock) {
        nameText.text = onlineManager.username
        informationText.text = OnlineManager.getInstance().failMessage

        if (avatarUri != onlineManager.avatarURL || avatarLoaded != onlineScoring.isAvatarLoaded) {
            avatarUri = onlineManager.avatarURL
            avatarLoaded = onlineScoring.isAvatarLoaded

            avatarSprite.textureRegion = if (!avatarLoaded || avatarUri.isEmpty()) {
                SkinTextures["emptyavatar"]
            } else {
                CachedTextures[avatarUri] ?: SkinTextures["emptyavatar"]
            }
        }

        super.onUpdate(clock)
    }
}
