package com.osudroid.ui.v2.settings

import com.reco1l.verktex.Platform
import com.osudroid.OsuPlatform
import com.osudroid.ui.v2.LoaderScene
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.form.FormSelect
import com.reco1l.verktex.ui.UIButton
import kotlin.math.max
import com.reco1l.verktex.ui.form.FormSlider
import com.reco1l.verktex.ui.form.FormInput
import com.reco1l.verktex.ui.form.FormControl
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.data.DatabaseManager
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.resources.Res
import com.osudroid.ui.v2.CalibrationScene
import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.mods.ModAutoplay
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.control.UISelect
import kotlinx.coroutines.Job
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import java.io.File


/**
 * Called when the size or position of the playfield change.
 */
internal var onPlayfieldChanged: (() -> Unit)? = null


val playerSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.mp_room_category_lobby),
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "player_team",
                title = Res.string.mp_room_category_teams_title,
                summary = Res.string.mp_room_category_teams_summary,
                entries = Res.array.mp_team_names,
                entryValues = Res.array.mp_team_values,
                initialValue = { Multiplayer.player?.team?.ordinal?.toString() ?: "" },
                onAttach = { (it as FormControl<*, *>).isEnabled = Multiplayer.room?.teamMode == TeamMode.TeamVS },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val team = (value as List<String>).firstOrNull()?.toIntOrNull()?.let { RoomTeam.entries.getOrNull(it) }
                    if (team != null) {
                        RoomAPI.setPlayerTeam(team)
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "player_preferModAcronym",
                title = Res.string.mp_room_prefer_mod_acronym_title,
                summary = Res.string.mp_room_prefer_mod_acronym_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.mp_room_category_in_game),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "player_submitScore",
                title = Res.string.mp_room_enable_score_submission_title,
                summary = Res.string.mp_room_enable_score_submission_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "player_nightcore",
                title = Res.string.mp_room_nightcore_title,
                summary = Res.string.mp_room_nightcore_summary,
                defaultValue = false,
                onChange = { value ->
                    Config.setUseNightcoreOnMultiplayer(value as Boolean)
                    Multiplayer.room?.let { Multiplayer.roomScene?.onRoomModsChange(it.mods) }
                }
            )
        )
    )
)

// Settings of the room, only for its host. They're the room's current values, not preferences.
val roomSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.mp_room_category_details),
        options = listOf(
            OptionInfo(
                type = OptionType.Input,
                key = "room_name",
                title = Res.string.mp_room_name_title,
                summary = Res.string.mp_room_name_summary,
                initialValue = { Multiplayer.room?.name ?: "" },
                // Applied once confirmed with the keyboard, not on every typed character.
                onAttach = {
                    val input = (it as FormInput).control
                    input.onConfirm = {
                        if (input.value.isNotBlank()) {
                            RoomAPI.setRoomName(input.value)
                        }
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "room_password",
                title = Res.string.mp_room_password_title,
                summary = Res.string.mp_room_password_summary,
                // The current password isn't known, only whether there's one.
                initialValue = { "" },
                // Applied once confirmed with the keyboard, not on every typed character.
                onAttach = {
                    val input = (it as FormInput).control
                    input.isPassword = true
                    input.onConfirm = { RoomAPI.setRoomPassword(input.value) }
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "room_max_players",
                title = Res.string.mp_room_max_players_title,
                summary = Res.string.mp_room_max_players_summary,
                max = 16f,
                step = 1f,
                initialValue = { Multiplayer.room?.maxPlayers ?: 2 },
                onAttach = {
                    // Not fewer than the players already in the room.
                    (it as FormSlider).control.min = max(2, Multiplayer.room?.activePlayers?.size ?: 0).toFloat()
                },
                onChange = { value -> RoomAPI.setRoomMaxPlayers((value as Float).toInt()) }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.mp_room_category_variables),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "room_free_mods",
                title = Res.string.mp_room_free_mods_title,
                summary = Res.string.mp_room_free_mods_summary,
                initialValue = { Multiplayer.room?.gameplaySettings?.isFreeMod ?: false },
                onChange = { value -> RoomAPI.setRoomFreeMods(value as Boolean) }
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "room_versus_mode",
                title = Res.string.mp_room_versus_mode_title,
                summary = Res.string.mp_room_versus_mode_summary,
                entries = Res.array.mp_team_versus_names,
                entryValues = Res.array.mp_team_versus_values,
                initialValue = { Multiplayer.room?.teamMode?.ordinal?.toString() ?: "" },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val mode = (value as List<String>).firstOrNull()?.toIntOrNull()?.let { TeamMode.entries.getOrNull(it) }
                    if (mode != null) {
                        RoomAPI.setRoomTeamMode(mode)
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "room_win_condition",
                title = Res.string.mp_room_win_condition_title,
                summary = Res.string.mp_room_win_condition_summary,
                entries = Res.array.mp_win_condition_names,
                entryValues = Res.array.mp_win_condition_values,
                initialValue = { Multiplayer.room?.winCondition?.ordinal?.toString() ?: "" },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val condition = (value as List<String>).firstOrNull()?.toIntOrNull()?.let { WinCondition.entries.getOrNull(it) }
                    if (condition != null) {
                        RoomAPI.setRoomWinCondition(condition)
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "room_removeSliderLock",
                title = Res.string.opt_remove_sliderlock_spinnerlock_title,
                summary = Res.string.opt_remove_sliderlock_spinnerlock_summary,
                initialValue = { Multiplayer.room?.gameplaySettings?.isRemoveSliderLock ?: false },
                onChange = { value -> RoomAPI.setRoomRemoveSliderLock(value as Boolean) }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.mp_room_category_share),
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "room_link",
                title = Res.string.mp_room_link_title,
                summary = Res.string.mp_room_link_summary,
                onClick = {
                    val room = Multiplayer.room
                    if (room != null) {
                        OsuPlatform.implementation.copyToClipboard(room.name, "${LobbyAPI.INVITE_HOST}/${room.id}/")
                        Toasts.show("Link copied to clipboard. If the room has a password, you can write it at the end of the link.")
                    }
                }
            )
        )
    )
)

val generalSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_online),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "stayOnline",
                title = Res.string.opt_stayonline_title,
                summary = Res.string.opt_stayonline_summary
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "loadAvatar",
                title = Res.string.opt_loadavatar_title,
                summary = Res.string.opt_loadavatar_summary
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "difficultyAlgorithm",
                title = Res.string.difficulty_algorithm_title,
                summary = Res.string.difficulty_algorithm_summary,
                // The default of Config.getDifficultyAlgorithm, shown while it was never changed.
                defaultValue = "0",
                entries = Res.array.difficulty_algorithm_names,
                entryValues = Res.array.difficulty_algorithm_values,
                onChange = { value ->
                    if (Multiplayer.isMultiplayer) {
                        @Suppress("UNCHECKED_CAST")
                        val newValue = (value as List<String>)[0]
                        Config.setString("difficultyAlgorithm", newValue)
                        Multiplayer.roomScene?.updateBeatmapInfo()
                    }
                }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_account),
        options = listOf(
            OptionInfo(
                type = OptionType.Input,
                key = "onlineUsername",
                title = Res.string.opt_login_title,
                summary = Res.string.opt_login_summary
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "onlinePassword",
                title = Res.string.opt_password_title,
                summary = Res.string.opt_password_summary,
                onAttach = {
                    (it as PreferenceInput).control.isPassword = true
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "registerAcc",
                title = Res.string.opt_register_title,
                summary = Res.string.opt_register_summary,
                onClick = {
                    Platform.openUrl(REGISTER_URL)
                }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_community),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "receiveAnnouncements",
                title = Res.string.opt_receive_announcements_title,
                summary = Res.string.opt_receive_announcements_summary
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_updates),
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "update",
                title = Res.string.opt_update_title,
                summary = Res.string.opt_update_summary,
                onClick = {
                    OsuPlatform.implementation.checkForUpdates(false)
                }
            )
        ),
        isVisible = OsuPlatform.implementation.canCheckForUpdates
    ),

    CategoryInfo(
        isVisible = OsuPlatform.implementation.canBackUpPreferences,
        title = StringTable.get(Res.string.opt_category_config_backup),
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "backup",
                title = Res.string.opt_config_backup_title,
                summary = Res.string.opt_config_backup_summary,
                onClick = {
                    val success = OsuPlatform.implementation.exportPreferences()
                    Toasts.showLong(StringTable.get(if (success) Res.string.config_backup_info_success else Res.string.config_backup_info_fail))
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "restore",
                title = Res.string.opt_config_backup_restore_title,
                summary = Res.string.opt_config_backup_restore_summary,
                onClick = {
                    val success = OsuPlatform.implementation.importPreferences()

                    if (success) {
                        val prefs = OsuPlatform.implementation.preferences
                        GlobalManager.getInstance().music.volume = prefs.getInt("bgmvolume", 100) / 100f

                        loadSkin(prefs.getString("skinPath", "")).invokeOnCompletion {
                            mainThread {
                                Toasts.showLong(StringTable.get(Res.string.config_backup_restore_info_success))
                            }
                        }
                    } else {
                        Toasts.showLong(StringTable.get(Res.string.config_backup_restore_info_fail))
                    }
                }
            )
        )
    ),

    CategoryInfo(
        isVisible = OsuPlatform.implementation.canChooseLanguage,
        title = StringTable.get(Res.string.opt_category_localization),
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "appLanguage",
                title = Res.string.opt_language_title,
                summary = Res.string.opt_language_summary,
                entries = Res.array.placeholder_array,
                entryValues = Res.array.placeholder_array,
                onAttach = {
                    // Changing it restarts the game, which would leave the room.
                    (it as PreferenceSelect).isEnabled = !Multiplayer.isMultiplayer

                    it.options = listOf(
                        UISelect.Option("system", "System Default"),
                        UISelect.Option("en", "English"),
                        // German
                        UISelect.Option("de", "Deutsch"),
                        // Spanish
                        UISelect.Option("es", "Español"),
                        // French
                        UISelect.Option("fr", "Français"),
                        // Indonesian
                        UISelect.Option("id", "Bahasa Indonesia"),
                        // Italian
                        UISelect.Option("it", "Italiano"),
                        // Japanese
                        UISelect.Option("ja", "日本語"),
                        // Korean
                        UISelect.Option("ko", "한국어"),
                        // Netherlands
                        UISelect.Option("nl", "Nederlands"),
                        // Portuguese
                        UISelect.Option("pt-rBR", "Português (Brasil)"),
                        // Russian
                        UISelect.Option("ru", "Русский"),
                        // Thai
                        UISelect.Option("th", "ไทย"),
                        // Vietnamese
                        UISelect.Option("vi", "Tiếng Việt"),
                        // Chinese
                        UISelect.Option("zh", "中文 (简体)"),
                    )
                },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val selected = (value as List<String>)[0]

                    OsuPlatform.implementation.setLanguage(selected)

                }
            )
        )
    )
)

val gameplaySection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_skin),
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "skinPath",
                title = Res.string.opt_skinpath_title,
                summary = Res.string.opt_skinpath_summary,
                entries = Res.array.placeholder_array,
                entryValues = Res.array.placeholder_array,
                // Config ends the path with a slash, which the folders don't, so it's shown and stored
                // as Config has it rather than through the preference.
                initialValue = { Config.getSkinPath() },
                onAttach = {
                    val skinMain = File(Config.getSkinTopPath())
                    val skins = Config.getSkins()!!.map { skin ->
                        UISelect.Option(skin.value.withTrailingSlash(), skin.key)
                    }.sortedBy { it.text }.toMutableList()
                    skins.add(0, UISelect.Option(skinMain.path.withTrailingSlash(), skinMain.name + " (Default)"))

                    @Suppress("UNCHECKED_CAST")
                    (it as FormSelect<String>).options = skins
                },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val newValue = (value as List<String>).firstOrNull() ?: return@OptionInfo
                    Config.setString("skinPath", newValue)
                    loadSkin(newValue)
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "hud_editor",
                title = Res.string.opt_hudEditor_title,
                summary = Res.string.opt_hudEditor_summary,
                // It plays the beatmap, which can't be done from a multiplayer room.
                onAttach = { (it as UIButton).isEnabled = !Multiplayer.isMultiplayer },
                onClick = {
                    val global = GlobalManager.getInstance()
                    val selectedBeatmap = global.selectedBeatmap

                    if (global.isHeadless) {
                        Toasts.showLong("The HUD editor needs the game running")
                    } else if (LibraryManager.getSizeOfBeatmaps() == 0 || selectedBeatmap == null) {
                        Toasts.showLong("Cannot enter HUD editor with empty beatmap library!")
                    } else {
                        val modMap = ModHashMap().apply {
                            put(ModAutoplay::class)
                        }

                        // The editor returns to the menu, as the settings closed before it.
                        (Verktex.scene as? SettingsMenu)?.back()

                        global.gameScene.setOldScene(MainScene)
                        global.gameScene.startGame(selectedBeatmap, null, modMap, true)
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "spinnerstyle",
                title = Res.string.opt_spinner_style_title,
                summary = Res.string.opt_spinner_style_summary,
                defaultValue = "1",
                entries = Res.array.spinner_style_names,
                entryValues = Res.array.spinner_style_values
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "skin",
                title = Res.string.opt_skin_title,
                summary = Res.string.opt_skin_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_hit_objects),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "showfirstapproachcircle",
                title = Res.string.opt_show_first_approach_circle_title,
                summary = Res.string.opt_show_first_approach_circle_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_background),
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "bgbrightness",
                title = Res.string.opt_bgbrightness_title,
                summary = Res.string.opt_bgbrightness_summary,
                defaultValue = 25,
                min = 0f,
                max = 100f,
                step = 0.1f,
                isFloat = true,
                valueFormatter = { "%.1f%%".format(it) }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "keepBackgroundAspectRatio",
                title = Res.string.opt_keep_background_aspect_ratio_title,
                summary = Res.string.opt_keep_background_aspect_ratio_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "enableStoryboard",
                title = Res.string.opt_enableStoryboard_title,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "enableVideo",
                title = Res.string.opt_video_title,
                summary = Res.string.opt_video_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_playfield),
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldSize",
                title = Res.string.opt_setplayfield_title,
                summary = Res.string.opt_setplayfield_summary,
                defaultValue = 100,
                min = 50f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = { value ->
                    Config.setPlayfieldSize(value as Float / 100f)
                    onPlayfieldChanged?.invoke()
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldHorizontalPosition",
                title = Res.string.opt_playfieldHorizontalPosition_title,
                summary = Res.string.opt_playfieldHorizontalPosition_summary,
                defaultValue = 50,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = { value ->
                    Config.setPlayfieldHorizontalPosition(value as Float / 100f)
                    onPlayfieldChanged?.invoke()
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldVerticalPosition",
                title = Res.string.opt_playfieldVerticalPosition_title,
                summary = Res.string.opt_playfieldVerticalPosition_summary,
                defaultValue = 50,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = { value ->
                    Config.setPlayfieldVerticalPosition(value as Float / 100f)
                    onPlayfieldChanged?.invoke()
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "displayPlayfieldBorder",
                title = Res.string.opt_display_playfield_border_title,
                summary = Res.string.opt_display_playfield_border_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_hud),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hideInGameUI",
                title = Res.string.opt_hide_ingame_ui_title,
                summary = Res.string.opt_hide_ingame_ui_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hideReplayMarquee",
                title = Res.string.opt_hide_replay_marquee_title,
                summary = Res.string.opt_hide_replay_marquee_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "fps",
                title = Res.string.opt_fps_title,
                summary = Res.string.opt_fps_summary,
                defaultValue = true,
                // Shows the engine's performance overlay.
                onChange = { value -> Settings[VerktexSettings.ShowPerformanceOverlay] = value as Boolean }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "displayScoreStatistics",
                title = Res.string.opt_display_score_statistics_title,
                summary = Res.string.opt_display_score_statistics_summary,
                defaultValue = true
            )
        )
    )
)

/**
 * The lowest resolution the game can render at, as the length of the short side of the screen.
 */
private const val MIN_RESOLUTION = 720

/**
 * The resolution scale option, from the scale giving [MIN_RESOLUTION] up to the resolution of the
 * display. Null if the display isn't larger than that.
 */
private fun resolutionScaleOption(): OptionInfo? {
    val display = Platform.currentDisplay
    val shortSide = min(display.width, display.height)

    if (shortSide <= MIN_RESOLUTION) {
        return null
    }

    val minPercentage = ceil(MIN_RESOLUTION * 100f / shortSide)

    // Applied once the slider is released, every value would make the surface again while dragging.
    var isDragging = false
    fun apply(percentage: Float) {
        Settings[VerktexSettings.ResolutionScale] = percentage / 100f
    }

    return OptionInfo(
        type = OptionType.Slider,
        key = RESOLUTION_SCALE_KEY,
        title = Res.string.opt_resolution_scale_title,
        summary = Res.string.opt_resolution_scale_summary,
        defaultValue = 100,
        min = minPercentage,
        max = 100f,
        step = 1f,
        valueFormatter = { percentage ->
            val width = (display.width * percentage / 100f).roundToInt()
            val height = (display.height * percentage / 100f).roundToInt()
            "${percentage.roundToInt()}% (${max(width, height)}x${min(width, height)})"
        },
        onChange = { value -> if (!isDragging) apply((value as Number).toFloat()) },
        onAttach = { component ->
            val slider = component as FormSlider
            slider.control.onStartDragging = { isDragging = true }
            slider.control.onStopDragging = {
                isDragging = false
                apply(slider.value)
            }
        }
    )
}

/**
 * The preference storing the resolution scale, as a percentage.
 */
const val RESOLUTION_SCALE_KEY = "resolutionScale"

/**
 * The preference storing the [GraphicsBackend] by name, loaded into [VerktexSettings.GraphicsBackend]
 * before the engine is initialized.
 */
const val GRAPHICS_BACKEND_KEY = "graphicsBackend"

private fun graphicsBackendOption(): OptionInfo {
    // The experimental Vulkan backend is only offered where the device reports Vulkan 1.1.
    val withVulkan = OsuPlatform.implementation.isVulkanAvailable

    return OptionInfo(
        type = OptionType.Select,
        key = GRAPHICS_BACKEND_KEY,
        title = Res.string.opt_graphics_backend_title,
        summary = Res.string.opt_graphics_backend_summary,
        entries = if (withVulkan) Res.array.opt_graphics_backend_names_with_vulkan else Res.array.opt_graphics_backend_names,
        entryValues = if (withVulkan) Res.array.opt_graphics_backend_values_with_vulkan else Res.array.opt_graphics_backend_values,
        defaultValue = GraphicsBackend.Auto.name,
        onChange = { value ->
            val selected = (value as? List<*>)?.firstOrNull() ?: value

            // The engine keeps the backend it was initialized with until the game restarts.
            if (selected == GraphicsBackend.Vulkan.name) {
                // Picking it again retries it, even if it crashed while starting the last time.
                OsuPlatform.implementation.resetVulkanCrashGuard()
                Toasts.showLong(StringTable.get(Res.string.opt_graphics_backend_vulkan_experimental))
            } else {
                Toasts.showLong(StringTable.get(Res.string.opt_graphics_backend_restart))
            }
        }
    )
}

val graphicsSection = listOfNotNull(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_rendering),
        options = listOfNotNull(graphicsBackendOption(), resolutionScaleOption())
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_cursor),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "showcursor",
                title = Res.string.opt_showcursor_title,
                summary = Res.string.opt_showcursor_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "cursorSize",
                title = Res.string.opt_cursor_size,
                summary = Res.string.opt_cursor_size_summary,
                defaultValue = 50,
                min = 25f,
                max = 300f,
                step = 1f,
                valueFormatter = { "${it / 100f}x" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "particles",
                title = Res.string.opt_particles_title,
                summary = Res.string.opt_particles_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_animations),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "dimHitObjects",
                title = Res.string.opt_dim_hit_objects_title,
                summary = Res.string.opt_dim_hit_objects_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "comboburst",
                title = Res.string.opt_combo_burst_title,
                summary = Res.string.opt_combo_burst_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "images",
                title = Res.string.opt_largeimages_title,
                summary = Res.string.opt_largeimages_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "animateFollowCircle",
                title = Res.string.opt_animate_follow_circle_title,
                summary = Res.string.opt_animate_follow_circle_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "animateComboText",
                title = Res.string.opt_animate_combo_text_title,
                summary = Res.string.opt_animate_combo_text_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "snakingInSliders",
                title = Res.string.opt_snakingInSliders_title,
                summary = Res.string.opt_snakingInSliders_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "snakingOutSliders",
                title = Res.string.opt_snakingOutSliders_title,
                summary = Res.string.opt_snakingOutSliders_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "noChangeDimInBreaks",
                title = Res.string.opt_noChangeDimInBreaks_title,
                summary = Res.string.opt_noChangeDimInBreaks_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "bursts",
                title = Res.string.opt_bursts_title,
                summary = Res.string.opt_bursts_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hitlighting",
                title = Res.string.opt_hitlighting_title,
                summary = Res.string.opt_hitlighting_summary,
                defaultValue = false
            )
        )
    )
)

val audioSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_volume),
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "bgmvolume",
                title = Res.string.opt_bgm_volume_title,
                summary = Res.string.opt_bgm_volume_summary,
                defaultValue = 100,
                min = 0f,
                max = 100f,
                step = 0.1f,
                isFloat = true,
                valueFormatter = { "%.1f%%".format(it) },
                onChange = { value ->
                    GlobalManager.getInstance().music.volume = value as Float / 100f
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "soundvolume",
                title = Res.string.opt_sound_volume_title,
                summary = Res.string.opt_sound_volume_summary,
                defaultValue = 100,
                min = 0f,
                max = 100f,
                step = 0.1f,
                isFloat = true,
                valueFormatter = { "%.1f%%".format(it) },
                onChange = { value ->
                    val floatValue = value as Float / 100f
                    // Set the configuration now as the sound below depends on this value.
                    Config.setSoundVolume(floatValue)

                    // Use the sound when the osu! cookie is clicked since it is guaranteed to be available and not
                    // skinnable (which means the player cannot silence it via skins).
                    SkinSounds.getDefault("menuhit").play()
                }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_offset),
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "offset",
                title = Res.string.opt_offset_title,
                summary = Res.string.opt_offset_summary,
                defaultValue = 0,
                min = -750f,
                max = 750f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "offset_calibration",
                title = Res.string.opt_offset_calibration_title,
                summary = Res.string.opt_offset_calibration_summary,
                onClick = {
                    CalibrationScene.OFFSET_MIN = -750
                    CalibrationScene.OFFSET_MAX = 750
                    CalibrationScene.show()
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "gameAudioSynchronizationThreshold",
                title = Res.string.opt_gameAudioSynchronizationThreshold_title,
                summary = Res.string.opt_gameAudioSynchronizationThreshold_summary,
                defaultValue = 20,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_effect),
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "metronomeswitch",
                title = Res.string.opt_metronome_switch_title,
                summary = Res.string.opt_metronome_switch_summary,
                defaultValue = "1",
                entries = Res.array.metronome_switch_names,
                entryValues = Res.array.metronome_switch_values
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "shiftPitchInRateChange",
                title = Res.string.opt_shiftPitchInRateChange_title,
                summary = Res.string.opt_shiftPitchInRateChange_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_miscellaneous),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "beatmapSounds",
                title = Res.string.opt_sound_title,
                summary = Res.string.opt_sound_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "musicpreview",
                title = Res.string.opt_musicpreview_title,
                summary = Res.string.opt_musicpreview_summary,
                defaultValue = true
            )
        )
    )
)

val librarySection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_import),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "preferNoVideoDownloads",
                title = Res.string.opt_prefer_no_video_downloads_title,
                summary = Res.string.opt_prefer_no_video_downloads_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteosz",
                title = Res.string.opt_deleteosz_title,
                summary = Res.string.opt_deleteosz_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "scandownload",
                title = Res.string.opt_scandownload_title,
                summary = Res.string.opt_scandownload_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteUnimportedBeatmaps",
                title = Res.string.opt_deleteUnimportedBeatmaps_title,
                summary = Res.string.opt_deleteUnimportedBeatmaps_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteUnsupportedVideos",
                title = Res.string.opt_delete_unsupported_videos_title,
                summary = Res.string.opt_delete_unsupported_videos_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "importReplay",
                title = Res.string.opt_import_replay_title,
                summary = Res.string.opt_import_replay_summary,
                onAttach = { it.isVisible = OsuPlatform.implementation.canImportReplays },
                onClick = { OsuPlatform.implementation.pickAndImportReplays() }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_metadata),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "forceromanized",
                title = Res.string.force_romanized,
                summary = Res.string.force_romanized_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_storage),
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "clear_beatmap_cache",
                title = Res.string.opt_clear_title,
                summary = Res.string.opt_clear_summary,
                onClick = {
                    LibraryManager.clearDatabase()
                    Toasts.showLong(StringTable.get(Res.string.library_cleared))
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "clear_properties",
                title = Res.string.opt_clearprops_title,
                summary = Res.string.opt_clearprops_summary,
                onClick = {
                    DatabaseManager.beatmapOptionsTable.deleteAll()
                }
            )
        )
    )
)

val inputSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_gameplay),
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "block_areas",
                title = Res.string.block_area_preference_title,
                summary = Res.string.block_area_preference_summary,
                onAttach = { it.isVisible = OsuPlatform.implementation.hasBlockAreaEditor },
                onClick = {
                    OsuPlatform.implementation.showBlockAreaEditor()
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "highPrecisionInput",
                title = Res.string.opt_highPrecisionInput_title,
                summary = Res.string.opt_highPrecisionInput_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "back_button_press_time",
                title = Res.string.opt_backButtonPressTime_title,
                summary = Res.string.opt_backButtonPressTime_summary,
                defaultValue = 300,
                min = 0f,
                max = 300f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "removeSliderLock",
                title = "Remove slider and spinner lock",
                summary = "[UNRANKED] Allow circles and sliders to be hittable when another slider or spinner is currently active.",
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        isVisible = OsuPlatform.implementation.vibrations != null,
        title = "Vibration",
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationCircle",
                title = "Circle",
                defaultValue = false,
                onChange = { value ->
                    OsuPlatform.implementation.vibrations?.isCircleVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationSlider",
                title = "Slider",
                defaultValue = false,
                onChange = { value ->
                    OsuPlatform.implementation.vibrations?.isSliderVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationSpinner",
                title = "Spinner",
                defaultValue = false,
                onChange = { value ->
                    OsuPlatform.implementation.vibrations?.isSpinnerVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "seekBarVibrateIntensity",
                title = Res.string.opt_seekBarVibrateIntensity_title,
                summary = Res.string.opt_seekBarVibrateIntensity_summary,
                defaultValue = 127,
                min = 1f,
                max = 255f,
                step = 1f,
                valueFormatter = { "${((it / 255f) * 100f).toInt()}%" },
                onChange = { value ->
                    OsuPlatform.implementation.vibrations?.intensity = (value as Float).toInt()
                }
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_synchronization),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "fixFrameOffset",
                title = Res.string.opt_fix_frame_offset_title,
                summary = Res.string.opt_fix_frame_offset_summary,
                defaultValue = true
            )
        )
    )
)

val advancedSection = listOf(
    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_directories),
        options = listOf(
            OptionInfo(
                type = OptionType.Input,
                key = "corePath",
                title = Res.string.opt_corepath_title,
                summary = Res.string.opt_corepath_summary
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "skinTopPath",
                title = Res.string.opt_skin_top_path_title,
                summary = Res.string.opt_skin_top_path_summary,
                onChange = { value ->
                    val newValue = value as String

                    if (newValue.trim { it <= ' ' }.isEmpty()) {
                        Config.loadConfig()
                        return@OptionInfo
                    }

                    val file = File(newValue)

                    if (!file.exists() && !file.mkdirs()) {
                        Toasts.showLong(StringTable.get(Res.string.message_error_dir_not_found))
                        return@OptionInfo
                    }

                    Config.loadConfig()
                }
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "directory",
                title = Res.string.opt_directory_title,
                summary = Res.string.opt_directory_summary,
                defaultValue = "/sdcard/osu!droid/Songs"
            )
        )
    ),

    CategoryInfo(
        title = StringTable.get(Res.string.opt_category_miscellaneous),
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "forceMaxRefreshRate",
                title = Res.string.opt_force_max_refresh_rate_title,
                summary = Res.string.opt_force_max_refresh_rate_summary,
                defaultValue = false,
                onAttach = { component ->
                    component.isVisible = OsuPlatform.implementation.canForceMaxRefreshRate
                },
                onChange = { value ->
                    OsuPlatform.implementation.applyRefreshRateSetting(value as Boolean)
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "safebeatmapbg",
                title = Res.string.opt_safe_beatmap_bg_title,
                summary = Res.string.opt_safe_beatmap_bg_summary,
                defaultValue = false
            )
        )
    )
)

// Constants
private fun String.withTrailingSlash() = if (endsWith('/')) this else "$this/"

const val REGISTER_URL: String = "https://${OnlineManager.hostname}/user/?action=register"

// Helper functions
fun loadSkin(path: String): Job {
    val loading = LoaderScene()
    loading.show()

    return async {
        try {
            BeatmapSkinManager.getInstance().clearSkin()
            Config.setSkinPath(path)
            Skin.load(path)
            SkinSounds.onSkinChanged()
            Verktex.withTreeLock { SkinTextures.onSkinChanged() }
        } finally {
            Verktex.withTreeLock {
                if (Verktex.scene == loading) {
                    Verktex.popScene()
                }
            }
        }

        Toasts.show(StringTable.get(Res.string.message_loaded_skin))
    }
}
