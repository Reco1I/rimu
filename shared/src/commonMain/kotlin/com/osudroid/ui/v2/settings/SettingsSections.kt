package com.osudroid.ui.v2.settings

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.game.GameplayVibrations
import com.osudroid.settings.SettingsBackup
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.resources.Languages
import com.reco1l.verktex.Platform
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
import com.osudroid.ui.v2.settings.SettingsOption.Type
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.control.UISelect
import kotlinx.coroutines.Job
import ru.nsu.ccfit.zuev.osu.LibraryManager
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import com.reco1l.verktex.localization.formatString
import okio.IOException
import okio.Path.Companion.toPath


/**
 * Called when the size or position of the playfield change.
 */
internal var onPlayfieldChanged: (() -> Unit)? = null


val playerSection = listOf(
    SettingsCategory(
        title = Res.string.mp_room_category_lobby,
        options = listOf(
            SettingsOption(
                type = Type.Select,
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
            SettingsOption(
                type = Type.Checkbox,
                key = "player_preferModAcronym",
                setting = OsuSettings.PreferModAcronymInMultiplayer,
                title = Res.string.mp_room_prefer_mod_acronym_title,
                summary = Res.string.mp_room_prefer_mod_acronym_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.mp_room_category_in_game,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "player_submitScore",
                setting = OsuSettings.SubmitScoreOnMultiplayer,
                title = Res.string.mp_room_enable_score_submission_title,
                summary = Res.string.mp_room_enable_score_submission_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "player_nightcore",
                setting = OsuSettings.UseNightcoreOnMultiplayer,
                title = Res.string.mp_room_nightcore_title,
                summary = Res.string.mp_room_nightcore_summary,
                onChange = {
                    Multiplayer.room?.let { Multiplayer.roomScene?.onRoomModsChange(it.mods) }
                }
            )
        )
    )
)

// Settings of the room, only for its host. They're the room's current values, not preferences.
val roomSection = listOf(
    SettingsCategory(
        title = Res.string.mp_room_category_details,
        options = listOf(
            SettingsOption(
                type = Type.Input,
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
            SettingsOption(
                type = Type.Input,
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
            SettingsOption(
                type = Type.Slider,
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

    SettingsCategory(
        title = Res.string.mp_room_category_variables,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "room_free_mods",
                title = Res.string.mp_room_free_mods_title,
                summary = Res.string.mp_room_free_mods_summary,
                initialValue = { Multiplayer.room?.gameplaySettings?.isFreeMod ?: false },
                onChange = { value -> RoomAPI.setRoomFreeMods(value as Boolean) }
            ),
            SettingsOption(
                type = Type.Select,
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
            SettingsOption(
                type = Type.Select,
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
            SettingsOption(
                type = Type.Checkbox,
                key = "room_removeSliderLock",
                title = Res.string.opt_remove_sliderlock_spinnerlock_title,
                summary = Res.string.opt_remove_sliderlock_spinnerlock_summary,
                initialValue = { Multiplayer.room?.gameplaySettings?.isRemoveSliderLock ?: false },
                onChange = { value -> RoomAPI.setRoomRemoveSliderLock(value as Boolean) }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.mp_room_category_share,
        options = listOf(
            SettingsOption(
                type = Type.Button,
                key = "room_link",
                title = Res.string.mp_room_link_title,
                summary = Res.string.mp_room_link_summary,
                onClick = {
                    val room = Multiplayer.room
                    if (room != null) {
                        Platform.copyToClipboard("${LobbyAPI.INVITE_HOST}/${room.id}/", label = room.name)
                        Toasts.show("Link copied to clipboard. If the room has a password, you can write it at the end of the link.")
                    }
                }
            )
        )
    )
)

val generalSection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_online,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "stayOnline",
                setting = OsuSettings.StayOnline,
                title = Res.string.opt_stayonline_title,
                summary = Res.string.opt_stayonline_summary
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "loadAvatar",
                setting = OsuSettings.LoadAvatar,
                title = Res.string.opt_loadavatar_title,
                summary = Res.string.opt_loadavatar_summary
            ),
            SettingsOption(
                type = Type.Select,
                key = "difficultyAlgorithm",
                setting = OsuSettings.DifficultyAlgorithmSetting,
                title = Res.string.difficulty_algorithm_title,
                summary = Res.string.difficulty_algorithm_summary,
                entries = Res.array.difficulty_algorithm_names,
                entryValues = Res.array.difficulty_algorithm_values,
                onChange = { value ->
                    if (Multiplayer.isMultiplayer) {
                        @Suppress("UNCHECKED_CAST")
                        val newValue = (value as List<String>)[0]
                        Multiplayer.roomScene?.updateBeatmapInfo()
                    }
                }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_account,
        options = listOf(
            SettingsOption(
                type = Type.Input,
                key = "onlineUsername",
                setting = OsuSettings.OnlineUsername,
                title = Res.string.opt_login_title,
                summary = Res.string.opt_login_summary
            ),
            SettingsOption(
                type = Type.Input,
                key = "onlinePassword",
                setting = OsuSettings.OnlinePassword,
                title = Res.string.opt_password_title,
                summary = Res.string.opt_password_summary,
                onAttach = {
                    (it as PreferenceInput).control.isPassword = true
                }
            ),
            SettingsOption(
                type = Type.Button,
                key = "registerAcc",
                title = Res.string.opt_register_title,
                summary = Res.string.opt_register_summary,
                onClick = {
                    Platform.openUrl(REGISTER_URL)
                }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_config_backup,
        options = listOf(
            SettingsOption(
                type = Type.Button,
                key = "backup",
                title = Res.string.opt_config_backup_title,
                summary = Res.string.opt_config_backup_summary,
                onClick = {
                    val success = SettingsBackup.export()
                    Toasts.showLong(if (success) Res.string.config_backup_info_success else Res.string.config_backup_info_fail)
                }
            ),
            SettingsOption(
                type = Type.Button,
                key = "restore",
                title = Res.string.opt_config_backup_restore_title,
                summary = Res.string.opt_config_backup_restore_summary,
                onClick = {
                    val success = SettingsBackup.import()

                    if (success) {
                        OsuGame.music.volume = OsuSettings.musicVolume

                        loadSkin(SkinFolders.path).invokeOnCompletion {
                            mainThread {
                                Toasts.showLong(Res.string.config_backup_restore_info_success)
                            }
                        }
                    } else {
                        Toasts.showLong(Res.string.config_backup_restore_info_fail)
                    }
                }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_localization,
        options = listOf(
            SettingsOption(
                type = Type.Select,
                key = "appLanguage",
                setting = OsuSettings.Language,
                title = Res.string.opt_language_title,
                summary = Res.string.opt_language_summary,
                entries = Res.array.placeholder_array,
                entryValues = Res.array.placeholder_array,
                onAttach = {
                    (it as PreferenceSelect).options = listOf(UISelect.Option(Languages.SYSTEM, "System Default")) +
                        Languages.names.map { (tag, name) -> UISelect.Option(tag, name) }
                },
                onChange = { value ->
                    // The text of the game follows it right away.
                    @Suppress("UNCHECKED_CAST")
                    Languages.apply((value as List<String>).firstOrNull() ?: Languages.SYSTEM)
                }
            )
        )
    )
)

val gameplaySection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_skin,
        options = listOf(
            SettingsOption(
                type = Type.Select,
                key = "skinPath",
                title = Res.string.opt_skinpath_title,
                summary = Res.string.opt_skinpath_summary,
                entries = Res.array.placeholder_array,
                entryValues = Res.array.placeholder_array,
                // SkinFolders ends the path with a slash, which the folders don't, so it's shown and
                // stored as SkinFolders has it rather than through the setting.
                initialValue = { SkinFolders.path },
                onAttach = {
                    val skinMain = SkinFolders.topPath.toPath()
                    val skins = SkinFolders.skins.map { skin ->
                        UISelect.Option(skin.value.withTrailingSlash(), skin.key)
                    }.sortedBy { it.text.toString() }.toMutableList()
                    skins.add(0, UISelect.Option(skinMain.toString().withTrailingSlash(), skinMain.name + " (Default)"))

                    @Suppress("UNCHECKED_CAST")
                    (it as FormSelect<String>).options = skins
                },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val newValue = (value as List<String>).firstOrNull() ?: return@SettingsOption
                    SkinFolders.path = newValue
                    loadSkin(newValue)
                }
            ),
            SettingsOption(
                type = Type.Button,
                key = "hud_editor",
                title = Res.string.opt_hudEditor_title,
                summary = Res.string.opt_hudEditor_summary,
                // It plays the beatmap, which can't be done from a multiplayer room.
                onAttach = { (it as UIButton).isEnabled = !Multiplayer.isMultiplayer },
                onClick = {
                    val selectedBeatmap = BeatmapSelection.beatmap

                    if (OsuGame.isHeadless) {
                        Toasts.showLong("The HUD editor needs the game running")
                    } else if (LibraryManager.getSizeOfBeatmaps() == 0 || selectedBeatmap == null) {
                        Toasts.showLong("Cannot enter HUD editor with empty beatmap library!")
                    } else {
                        val modMap = ModHashMap().apply {
                            put(ModAutoplay::class)
                        }

                        // The editor returns to the menu, as the settings closed before it.
                        (Verktex.scene as? SettingsMenu)?.back()

                        OsuGame.createGameScene().startGame(selectedBeatmap, null, modMap, true)
                    }
                }
            ),
            SettingsOption(
                type = Type.Select,
                key = "spinnerstyle",
                setting = OsuSettings.SpinnerStyle,
                title = Res.string.opt_spinner_style_title,
                summary = Res.string.opt_spinner_style_summary,
                entries = Res.array.spinner_style_names,
                entryValues = Res.array.spinner_style_values
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "skin",
                setting = OsuSettings.UseCustomSkins,
                title = Res.string.opt_skin_title,
                summary = Res.string.opt_skin_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_hit_objects,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "showfirstapproachcircle",
                setting = OsuSettings.ShowFirstApproachCircle,
                title = Res.string.opt_show_first_approach_circle_title,
                summary = Res.string.opt_show_first_approach_circle_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_background,
        options = listOf(
            SettingsOption(
                type = Type.Slider,
                key = "bgbrightness",
                setting = OsuSettings.BackgroundBrightness,
                title = Res.string.opt_bgbrightness_title,
                summary = Res.string.opt_bgbrightness_summary,
                min = 0f,
                max = 100f,
                step = 0.1f,
                valueFormatter = { formatString("%.1f%%", it) }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "keepBackgroundAspectRatio",
                setting = OsuSettings.KeepBackgroundAspectRatio,
                title = Res.string.opt_keep_background_aspect_ratio_title,
                summary = Res.string.opt_keep_background_aspect_ratio_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "enableStoryboard",
                setting = OsuSettings.EnableStoryboard,
                title = Res.string.opt_enableStoryboard_title,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "enableVideo",
                setting = OsuSettings.EnableVideo,
                title = Res.string.opt_video_title,
                summary = Res.string.opt_video_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_playfield,
        options = listOf(
            SettingsOption(
                type = Type.Slider,
                key = "playfieldSize",
                setting = OsuSettings.PlayfieldSize,
                title = Res.string.opt_setplayfield_title,
                summary = Res.string.opt_setplayfield_summary,
                min = 50f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = {
                    onPlayfieldChanged?.invoke()
                }
            ),
            SettingsOption(
                type = Type.Slider,
                key = "playfieldHorizontalPosition",
                setting = OsuSettings.PlayfieldHorizontalPosition,
                title = Res.string.opt_playfieldHorizontalPosition_title,
                summary = Res.string.opt_playfieldHorizontalPosition_summary,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = {
                    onPlayfieldChanged?.invoke()
                }
            ),
            SettingsOption(
                type = Type.Slider,
                key = "playfieldVerticalPosition",
                setting = OsuSettings.PlayfieldVerticalPosition,
                title = Res.string.opt_playfieldVerticalPosition_title,
                summary = Res.string.opt_playfieldVerticalPosition_summary,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}%" },
                onChange = {
                    onPlayfieldChanged?.invoke()
                }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "displayPlayfieldBorder",
                setting = OsuSettings.DisplayPlayfieldBorder,
                title = Res.string.opt_display_playfield_border_title,
                summary = Res.string.opt_display_playfield_border_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_hud,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "hideInGameUI",
                setting = OsuSettings.HideInGameUI,
                title = Res.string.opt_hide_ingame_ui_title,
                summary = Res.string.opt_hide_ingame_ui_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "hideReplayMarquee",
                setting = OsuSettings.HideReplayMarquee,
                title = Res.string.opt_hide_replay_marquee_title,
                summary = Res.string.opt_hide_replay_marquee_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "fps",
                setting = OsuSettings.ShowFps,
                title = Res.string.opt_fps_title,
                summary = Res.string.opt_fps_summary,
                // Shows the engine's performance overlay.
                onChange = { value -> Settings[VerktexSettings.ShowPerformanceOverlay] = value as Boolean }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "displayScoreStatistics",
                setting = OsuSettings.DisplayScoreStatistics,
                title = Res.string.opt_display_score_statistics_title,
                summary = Res.string.opt_display_score_statistics_summary,
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
private fun resolutionScaleOption(): SettingsOption? {
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

    return SettingsOption(
        type = Type.Slider,
        key = "resolutionScale",
        setting = OsuSettings.ResolutionScale,
        title = Res.string.opt_resolution_scale_title,
        summary = Res.string.opt_resolution_scale_summary,
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

private fun graphicsBackendOption(): SettingsOption? {
    val backends = Platform.availableGraphicsBackends
    if (backends.size < 2) {
        return null
    }

    // The experimental Vulkan backend is only offered where the device reports Vulkan 1.1.
    val withVulkan = GraphicsBackend.Vulkan in backends

    return SettingsOption(
        type = Type.Select,
        key = "graphicsBackend",
        // The engine's own setting, read when it starts.
        setting = VerktexSettings.GraphicsBackend,
        title = Res.string.opt_graphics_backend_title,
        summary = Res.string.opt_graphics_backend_summary,
        entries = if (withVulkan) Res.array.opt_graphics_backend_names_with_vulkan else Res.array.opt_graphics_backend_names,
        entryValues = if (withVulkan) Res.array.opt_graphics_backend_values_with_vulkan else Res.array.opt_graphics_backend_values,
        onChange = { value ->
            val selected = (value as? List<*>)?.firstOrNull() ?: value

            // The engine keeps the backend it was initialized with until the game restarts.
            if (selected == GraphicsBackend.Vulkan.name) {
                Toasts.showLong(Res.string.opt_graphics_backend_vulkan_experimental)
            } else {
                Toasts.showLong(Res.string.opt_graphics_backend_restart)
            }
        }
    )
}

val graphicsSection = listOfNotNull(
    SettingsCategory(
        title = Res.string.opt_category_rendering,
        options = listOfNotNull(graphicsBackendOption(), resolutionScaleOption())
    ),

    SettingsCategory(
        title = Res.string.opt_category_cursor,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "showcursor",
                setting = OsuSettings.ShowCursor,
                title = Res.string.opt_showcursor_title,
                summary = Res.string.opt_showcursor_summary,
            ),
            SettingsOption(
                type = Type.Slider,
                key = "cursorSize",
                setting = OsuSettings.CursorSize,
                title = Res.string.opt_cursor_size,
                summary = Res.string.opt_cursor_size_summary,
                min = 25f,
                max = 300f,
                step = 1f,
                valueFormatter = { "${it / 100f}x" }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "particles",
                setting = OsuSettings.UseParticles,
                title = Res.string.opt_particles_title,
                summary = Res.string.opt_particles_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_animations,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "dimHitObjects",
                setting = OsuSettings.DimHitObjects,
                title = Res.string.opt_dim_hit_objects_title,
                summary = Res.string.opt_dim_hit_objects_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "comboburst",
                setting = OsuSettings.ComboBurst,
                title = Res.string.opt_combo_burst_title,
                summary = Res.string.opt_combo_burst_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "images",
                setting = OsuSettings.Corovans,
                title = Res.string.opt_largeimages_title,
                summary = Res.string.opt_largeimages_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "animateFollowCircle",
                setting = OsuSettings.AnimateFollowCircle,
                title = Res.string.opt_animate_follow_circle_title,
                summary = Res.string.opt_animate_follow_circle_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "animateComboText",
                setting = OsuSettings.AnimateComboText,
                title = Res.string.opt_animate_combo_text_title,
                summary = Res.string.opt_animate_combo_text_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "snakingInSliders",
                setting = OsuSettings.SnakingInSliders,
                title = Res.string.opt_snakingInSliders_title,
                summary = Res.string.opt_snakingInSliders_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "snakingOutSliders",
                setting = OsuSettings.SnakingOutSliders,
                title = Res.string.opt_snakingOutSliders_title,
                summary = Res.string.opt_snakingOutSliders_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "noChangeDimInBreaks",
                setting = OsuSettings.NoChangeDimInBreaks,
                title = Res.string.opt_noChangeDimInBreaks_title,
                summary = Res.string.opt_noChangeDimInBreaks_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "bursts",
                setting = OsuSettings.BurstEffects,
                title = Res.string.opt_bursts_title,
                summary = Res.string.opt_bursts_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "hitlighting",
                setting = OsuSettings.HitLighting,
                title = Res.string.opt_hitlighting_title,
                summary = Res.string.opt_hitlighting_summary,
            )
        )
    )
)

val audioSection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_volume,
        options = listOf(
            SettingsOption(
                type = Type.Slider,
                key = "bgmvolume",
                setting = OsuSettings.MusicVolume,
                title = Res.string.opt_bgm_volume_title,
                summary = Res.string.opt_bgm_volume_summary,
                min = 0f,
                max = 100f,
                step = 0.1f,
                valueFormatter = { formatString("%.1f%%", it) },
                onChange = { value ->
                    OsuGame.music.volume = value as Float / 100f
                }
            ),
            SettingsOption(
                type = Type.Slider,
                key = "soundvolume",
                setting = OsuSettings.SoundVolume,
                title = Res.string.opt_sound_volume_title,
                summary = Res.string.opt_sound_volume_summary,
                min = 0f,
                max = 100f,
                step = 0.1f,
                valueFormatter = { formatString("%.1f%%", it) },
                onChange = {
                    // Use the sound when the osu! cookie is clicked since it is guaranteed to be available and not
                    // skinnable (which means the player cannot silence it via skins).
                    SkinSounds.getDefault("menuhit").play()
                }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_offset,
        options = listOf(
            SettingsOption(
                type = Type.Slider,
                key = "offset",
                setting = OsuSettings.Offset,
                title = Res.string.opt_offset_title,
                summary = Res.string.opt_offset_summary,
                min = -750f,
                max = 750f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            SettingsOption(
                type = Type.Button,
                key = "offset_calibration",
                title = Res.string.opt_offset_calibration_title,
                summary = Res.string.opt_offset_calibration_summary,
                onClick = {
                    CalibrationScene(offsetMin = -750, offsetMax = 750).show()
                }
            ),
            SettingsOption(
                type = Type.Slider,
                key = "gameAudioSynchronizationThreshold",
                setting = OsuSettings.GameAudioSynchronizationThreshold,
                title = Res.string.opt_gameAudioSynchronizationThreshold_title,
                summary = Res.string.opt_gameAudioSynchronizationThreshold_summary,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_effect,
        options = listOf(
            SettingsOption(
                type = Type.Select,
                key = "metronomeswitch",
                setting = OsuSettings.MetronomeSwitch,
                title = Res.string.opt_metronome_switch_title,
                summary = Res.string.opt_metronome_switch_summary,
                entries = Res.array.metronome_switch_names,
                entryValues = Res.array.metronome_switch_values
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "shiftPitchInRateChange",
                setting = OsuSettings.ShiftPitchInRateChange,
                title = Res.string.opt_shiftPitchInRateChange_title,
                summary = Res.string.opt_shiftPitchInRateChange_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_miscellaneous,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "beatmapSounds",
                setting = OsuSettings.UseBeatmapSounds,
                title = Res.string.opt_sound_title,
                summary = Res.string.opt_sound_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "musicpreview",
                setting = OsuSettings.PlayMusicPreview,
                title = Res.string.opt_musicpreview_title,
                summary = Res.string.opt_musicpreview_summary,
            )
        )
    )
)

val librarySection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_import,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "preferNoVideoDownloads",
                setting = OsuSettings.PreferNoVideoDownloads,
                title = Res.string.opt_prefer_no_video_downloads_title,
                summary = Res.string.opt_prefer_no_video_downloads_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "deleteosz",
                setting = OsuSettings.DeleteOsz,
                title = Res.string.opt_deleteosz_title,
                summary = Res.string.opt_deleteosz_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "scandownload",
                setting = OsuSettings.ScanDownload,
                title = Res.string.opt_scandownload_title,
                summary = Res.string.opt_scandownload_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "deleteUnimportedBeatmaps",
                setting = OsuSettings.DeleteUnimportedBeatmaps,
                title = Res.string.opt_deleteUnimportedBeatmaps_title,
                summary = Res.string.opt_deleteUnimportedBeatmaps_summary,
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "deleteUnsupportedVideos",
                setting = OsuSettings.DeleteUnsupportedVideos,
                title = Res.string.opt_delete_unsupported_videos_title,
                summary = Res.string.opt_delete_unsupported_videos_summary,
            ),
            SettingsOption(
                type = Type.Button,
                key = "importReplay",
                title = Res.string.opt_import_replay_title,
                summary = Res.string.opt_import_replay_summary,
                onAttach = { it.isVisible = Platform.canPickFiles },
                onClick = { pickAndImportReplays() }
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_metadata,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "forceromanized",
                setting = OsuSettings.ForceRomanized,
                title = Res.string.force_romanized,
                summary = Res.string.force_romanized_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_storage,
        options = listOf(
            SettingsOption(
                type = Type.Button,
                key = "clear_beatmap_cache",
                title = Res.string.opt_clear_title,
                summary = Res.string.opt_clear_summary,
                onClick = {
                    async {
                        LibraryManager.clearDatabase()
                        Toasts.showLong(Res.string.library_cleared)
                    }
                }
            ),
            SettingsOption(
                type = Type.Button,
                key = "clear_properties",
                title = Res.string.opt_clearprops_title,
                summary = Res.string.opt_clearprops_summary,
                onClick = {
                    async { DatabaseManager.beatmapOptionsTable.deleteAll() }
                }
            )
        )
    )
)

val inputSection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_gameplay,
        options = listOf(
            SettingsOption(
                type = Type.Button,
                key = "block_areas",
                title = Res.string.block_area_preference_title,
                summary = Res.string.block_area_preference_summary,
                onClick = { BlockAreaEditor.show() }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "highPrecisionInput",
                setting = OsuSettings.HighPrecisionInput,
                title = Res.string.opt_highPrecisionInput_title,
                summary = Res.string.opt_highPrecisionInput_summary,
            ),
            SettingsOption(
                type = Type.Slider,
                key = "back_button_press_time",
                setting = OsuSettings.BackButtonPressTime,
                title = Res.string.opt_backButtonPressTime_title,
                summary = Res.string.opt_backButtonPressTime_summary,
                min = 0f,
                max = 300f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "removeSliderLock",
                setting = OsuSettings.RemoveSliderLock,
                title = "Remove slider and spinner lock",
                summary = "[UNRANKED] Allow circles and sliders to be hittable when another slider or spinner is currently active.",
            )
        )
    ),

    SettingsCategory(
        isVisible = GameplayVibrations.isAvailable,
        title = "Vibration",
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "vibrationCircle",
                setting = OsuSettings.VibrationCircle,
                title = "Circle",
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "vibrationSlider",
                setting = OsuSettings.VibrationSlider,
                title = "Slider",
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "vibrationSpinner",
                setting = OsuSettings.VibrationSpinner,
                title = "Spinner",
            ),
            SettingsOption(
                type = Type.Slider,
                key = "seekBarVibrateIntensity",
                setting = OsuSettings.SeekBarVibrateIntensity,
                title = Res.string.opt_seekBarVibrateIntensity_title,
                summary = Res.string.opt_seekBarVibrateIntensity_summary,
                min = 1f,
                max = 255f,
                step = 1f,
                valueFormatter = { "${((it / 255f) * 100f).toInt()}%" },
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_synchronization,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "fixFrameOffset",
                setting = OsuSettings.FixFrameOffset,
                title = Res.string.opt_fix_frame_offset_title,
                summary = Res.string.opt_fix_frame_offset_summary,
            )
        )
    )
)

val advancedSection = listOf(
    SettingsCategory(
        title = Res.string.opt_category_directories,
        options = listOf(
            SettingsOption(
                type = Type.Input,
                key = "corePath",
                setting = OsuSettings.CorePath,
                title = Res.string.opt_corepath_title,
                summary = Res.string.opt_corepath_summary
            ),
            SettingsOption(
                type = Type.Input,
                key = "skinTopPath",
                setting = OsuSettings.SkinTopPath,
                title = Res.string.opt_skin_top_path_title,
                summary = Res.string.opt_skin_top_path_summary,
                onChange = { value ->
                    val newValue = value as String

                    if (newValue.trim { it <= ' ' }.isEmpty()) {
                        return@SettingsOption
                    }

                    val folder = newValue.toPath()
                    val exists = try {
                        Platform.fileSystem.createDirectories(folder)
                        true
                    } catch (e: IOException) {
                        false
                    }

                    if (!exists) {
                        Toasts.showLong(Res.string.message_error_dir_not_found)
                        return@SettingsOption
                    }

                    SkinFolders.load()
                }
            ),
            SettingsOption(
                type = Type.Input,
                key = "directory",
                setting = OsuSettings.BeatmapPath,
                title = Res.string.opt_directory_title,
                summary = Res.string.opt_directory_summary,
            )
        )
    ),

    SettingsCategory(
        title = Res.string.opt_category_miscellaneous,
        options = listOf(
            SettingsOption(
                type = Type.Checkbox,
                key = "forceMaxRefreshRate",
                setting = OsuSettings.ForceMaxRefreshRate,
                title = Res.string.opt_force_max_refresh_rate_title,
                summary = Res.string.opt_force_max_refresh_rate_summary,
                onAttach = { component ->
                    component.isVisible = Platform.canForceMaxRefreshRate
                },
                onChange = { value ->
                    Platform.forceMaxRefreshRate = value as Boolean
                }
            ),
            SettingsOption(
                type = Type.Checkbox,
                key = "safebeatmapbg",
                setting = OsuSettings.SafeBeatmapBackground,
                title = Res.string.opt_safe_beatmap_bg_title,
                summary = Res.string.opt_safe_beatmap_bg_summary,
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
            SkinFolders.path = path
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

        Toasts.show(Res.string.message_loaded_skin)
    }
}
