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
import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.resources.R
import com.osudroid.utils.mainThread
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.util.Locale
import kotlin.math.roundToInt
import com.reco1l.verktex.ui.themed

class PlayerButton : UIClickableContainer() {

    private val onlineManager = OnlineManager.getInstance()
    private val onlineScoring = OnlineScoring.getInstance()

    private var avatarUri = ""
    private var avatarLoaded = false

    private lateinit var nameText: UIText
    private lateinit var informationText: UIText
    private lateinit var avatarSprite: UISprite


    init {
        onActionUp = {
            // Like the old online panel, the profile is only reachable while logged in.
            if (onlineManager.isStayOnline) {
                UIConfirmDialog().apply {
                    title = onlineManager.username
                    text = StringTable.get(R.string.dialog_visit_osudroid_profile_page)
                    onConfirm = {
                        mainThread {
                            WebViewFragment().setURL(WebViewFragment.PROFILE_URL + onlineManager.userId).show()
                        }
                    }
                }.show()
            }
        }

        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            minWidth = 208f.dip
            padding = Padding(9f.dip)
            spacing = 8f.dip
            radius = themed { radius.xl }
            backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.2f) }

            avatarSprite = sprite {
                width = 48f.dip
                height = 48f.dip
                backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.3f) }
                radius = themed { radius.xl }
                scaleType = UISprite.ScaleType.Crop
                textureRegion = SkinTextures["emptyavatar"]
            }

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = UILinearContainer.Orientation.Vertical

                nameText = text {
                    text = "Player"
                    color = themed { palette.accent }
                }

                informationText = text {
                    text = "Loading..."
                    fontSize = themed { typography.smallCaption.size }
                    color = themed { palette.accent.shade(0.8f) }
                }
            }
        }
    }

    override fun onUpdate(clock: FrameClock) {
        nameText.text = onlineManager.username

        // Logged in it shows the player's statistics, otherwise why it isn't.
        if (onlineManager.isStayOnline && onlineManager.userId > 0) {
            informationText.text = "#%,d · %,dpp · %.2f%%".format(
                Locale.US,
                onlineManager.rank,
                onlineManager.pp.roundToInt(),
                onlineManager.accuracy * 100f
            )
        } else {
            informationText.text = onlineManager.failMessage
        }

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
