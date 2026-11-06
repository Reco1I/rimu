package com.osudroid.settings

import com.osudroid.OsuGameHost
import com.osudroid.resources.Languages
import com.osudroid.ui.v2.SkinFolders
import com.reco1l.verktex.Logger
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.settings.VerktexSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import ru.nsu.ccfit.zuev.osu.BuildType
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode

/**
 * A setting of the game, see [OsuSettings].
 *
 * @param bounds The range a whole number setting must be in, which values from a backup are kept in.
 * @param isSensitive Whether the setting is private to this installation (e.g. the account), so it's
 * left out of backups.
 */
class OsuSettingKey<T>(
    name: String,
    default: T,
    serializer: KSerializer<T>,
    val bounds: IntRange? = null,
    val isSensitive: Boolean = false,
) : SettingKey<T>(name, default, serializer)

/**
 * The settings of the game, stored by Verktex's [Settings] under the names the game's preferences
 * always had, so older values and backups keep working.
 *
 * Read one with `Settings[OsuSettings.X]`, or with the properties below for the ones read in another
 * unit than they're stored in.
 */
object OsuSettings {

    private val keys = LinkedHashMap<String, OsuSettingKey<*>>()

    private inline fun <reified T> key(name: String, default: T, bounds: IntRange? = null, isSensitive: Boolean = false) =
        OsuSettingKey(name, default, serializer<T>(), bounds, isSensitive).also { keys[name] = it }

    /**
     * Every setting of the game, by name.
     */
    @JvmStatic
    val all: Map<String, OsuSettingKey<*>>
        get() = keys


    //region Graphics

    @JvmField val UseCustomSkins = key("skin", false)
    @JvmField val UseBeatmapSounds = key("beatmapSounds", true)
    @JvmField val ComboBurst = key("comboburst", false)
    @JvmField val Corovans = key("images", false)
    @JvmField val ShowFps = key("fps", true)
    @JvmField val SpinnerStyle = key("spinnerstyle", "1")
    @JvmField val ShowFirstApproachCircle = key("showfirstapproachcircle", false)
    @JvmField val EnableStoryboard = key("enableStoryboard", false)
    @JvmField val EnableVideo = key("enableVideo", false)
    @JvmField val KeepBackgroundAspectRatio = key("keepBackgroundAspectRatio", false)
    @JvmField val NoChangeDimInBreaks = key("noChangeDimInBreaks", false)
    @JvmField val DimHitObjects = key("dimHitObjects", true)
    @JvmField val ForceMaxRefreshRate = key("forceMaxRefreshRate", false)
    @JvmField val PlayfieldSize = key("playfieldSize", 100, bounds = 50..100)
    @JvmField val PlayfieldHorizontalPosition = key("playfieldHorizontalPosition", 50, bounds = 0..100)
    @JvmField val PlayfieldVerticalPosition = key("playfieldVerticalPosition", 50, bounds = 0..100)
    @JvmField val DisplayPlayfieldBorder = key("displayPlayfieldBorder", false)
    @JvmField val AnimateFollowCircle = key("animateFollowCircle", true)
    @JvmField val AnimateComboText = key("animateComboText", true)
    @JvmField val SnakingInSliders = key("snakingInSliders", true)
    @JvmField val SnakingOutSliders = key("snakingOutSliders", true)
    @JvmField val BackgroundBrightness = key("bgbrightness", 25f, bounds = 0..100)
    @JvmField val CursorSize = key("cursorSize", 50, bounds = 25..300)
    @JvmField val BurstEffects = key("bursts", false)
    @JvmField val HitLighting = key("hitlighting", false)
    @JvmField val UseParticles = key("particles", false)
    @JvmField val UseCustomComboColors = key("useCustomColors", false)
    @JvmField val ComboColor1 = key("combo1", 0xff000000.toInt())
    @JvmField val ComboColor2 = key("combo2", 0xff000000.toInt())
    @JvmField val ComboColor3 = key("combo3", 0xff000000.toInt())
    @JvmField val ComboColor4 = key("combo4", 0xff000000.toInt())
    @JvmField val ShowCursor = key("showcursor", false)
    @JvmField val HideInGameUI = key("hideInGameUI", false)
    @JvmField val HideReplayMarquee = key("hideReplayMarquee", false)
    @JvmField val DisplayScoreStatistics = key("displayScoreStatistics", false)
    @JvmField val ShowScoreboard = key("showscoreboard", true)
    @JvmField val SafeBeatmapBackground = key("safebeatmapbg", false)
    @JvmField val UseLegacyResolutionPolicy = key("use_legacy_resolution_policy", true)

    /**
     * The resolution scale, as a percentage, which the engine's own setting follows (see
     * [VerktexSettings.ResolutionScale]).
     */
    @JvmField val ResolutionScale = key("resolutionScale", 100)

    //endregion

    //region Audio

    @JvmField val MusicVolume = key("bgmvolume", 100f, bounds = 0..100)
    @JvmField val SoundVolume = key("soundvolume", 100f, bounds = 0..100)
    @JvmField val Offset = key("offset", 0, bounds = -750..750)
    @JvmField val GameAudioSynchronizationThreshold = key("gameAudioSynchronizationThreshold", 20)
    @JvmField val MetronomeSwitch = key("metronomeswitch", "1")
    @JvmField val ShiftPitchInRateChange = key("shiftPitchInRateChange", false)
    @JvmField val PlayMusicPreview = key("musicpreview", true)

    //endregion

    //region Library

    @JvmField val CorePath = key("corePath", "")
    @JvmField val SkinPath = key("skinPath", "")
    @JvmField val SkinTopPath = key("skinTopPath", "")
    @JvmField val BeatmapPath = key("directory", "")
    @JvmField val DeleteOsz = key("deleteosz", true)
    @JvmField val ScanDownload = key("scandownload", false)
    @JvmField val DeleteUnimportedBeatmaps = key("deleteUnimportedBeatmaps", false)
    @JvmField val DeleteUnsupportedVideos = key("deleteUnsupportedVideos", true)
    @JvmField val ForceRomanized = key("forceromanized", false)
    @JvmField val PreferNoVideoDownloads = key("preferNoVideoDownloads", false)
    @JvmField val BeatmapMirror = key("beatmapMirror", 0)
    @JvmField val SongSelectSortOrder = key("songSelectSortOrder", 0)
    @JvmField val SongSelectGrouping = key("songSelectGrouping", 0)

    //endregion

    //region Gameplay and input

    @JvmField val FixFrameOffset = key("fixFrameOffset", true)
    @JvmField val RemoveSliderLock = key("removeSliderLock", false)
    @JvmField val HighPrecisionInput = key("highPrecisionInput", false)
    @JvmField val BackButtonPressTime = key("back_button_press_time", 300, bounds = 0..300)
    @JvmField val VibrationCircle = key("vibrationCircle", false)
    @JvmField val VibrationSlider = key("vibrationSlider", false)
    @JvmField val VibrationSpinner = key("vibrationSpinner", false)
    @JvmField val SeekBarVibrateIntensity = key("seekBarVibrateIntensity", 127, bounds = 1..255)

    //endregion

    //region Online and multiplayer

    @JvmField val OnlineUsername = key("onlineUsername", "", isSensitive = true)
    @JvmField val OnlinePassword = key("onlinePassword", "", isSensitive = true)
    @JvmField val StayOnline = key("stayOnline", false)
    @JvmField val LoadAvatar = key("loadAvatar", false)
    @JvmField val ReceiveAnnouncements = key("receiveAnnouncements", true)
    @JvmField val DifficultyAlgorithmSetting = key("difficultyAlgorithm", "0")
    @JvmField val BeatmapLeaderboardScoringModeSetting = key("beatmapLeaderboardScoringMode", "0")
    @JvmField val UseNightcoreOnMultiplayer = key("player_nightcore", false)
    @JvmField val SubmitScoreOnMultiplayer = key("player_submitScore", true)
    @JvmField val PreferModAcronymInMultiplayer = key("player_preferModAcronym", false)

    //endregion

    //region Application

    @JvmField val Language = key("appLanguage", "system")
    @JvmField val InstallId = key("installID", "", isSensitive = true)
    @JvmField val Version = key("version", -1L, isSensitive = true)
    @JvmField val DroidStarRatingVersion = key("droidStarRatingVersion", 0L, isSensitive = true)
    @JvmField val StandardStarRatingVersion = key("standardStarRatingVersion", 0L, isSensitive = true)

    /**
     * The star rating version before each algorithm had its own, see [DroidStarRatingVersion].
     */
    @JvmField val LegacyStarRatingVersion = key("starRatingVersion", 0L)

    /**
     * The name of the graphics backend the game's preferences had, imported into the engine's own
     * setting, see [importLegacyPreferences].
     */
    @JvmField val LegacyGraphicsBackend = key("graphicsBackend", "Auto")

    //endregion


    //region Values in other units

    /**
     * The volume of the music, from 0 to 1.
     */
    @JvmStatic
    val musicVolume: Float
        get() = Settings[MusicVolume] / 100f

    /**
     * The volume of the sound effects, from 0 to 1.
     */
    @JvmStatic
    val soundVolume: Float
        get() = Settings[SoundVolume] / 100f

    /**
     * The brightness of the background during gameplay, from 0 to 1.
     */
    @JvmStatic
    val backgroundBrightness: Float
        get() = Settings[BackgroundBrightness] / 100f

    /**
     * The audio offset, in milliseconds.
     */
    @JvmStatic
    val offset: Float
        get() = Settings[Offset].toFloat()

    /**
     * The size of the cursor, 1 being its normal size.
     */
    @JvmStatic
    val cursorSize: Float
        get() = Settings[CursorSize] / 100f

    /**
     * The size of the playfield, from 0.5 to 1 of the screen.
     */
    @JvmStatic
    val playfieldSize: Float
        get() = Settings[PlayfieldSize] / 100f

    /**
     * Where the playfield is horizontally, from 0 (left) to 1 (right).
     */
    @JvmStatic
    val playfieldHorizontalPosition: Float
        get() = Settings[PlayfieldHorizontalPosition] / 100f

    /**
     * Where the playfield is vertically, from 0 (top) to 1 (bottom).
     */
    @JvmStatic
    val playfieldVerticalPosition: Float
        get() = Settings[PlayfieldVerticalPosition] / 100f

    @JvmStatic
    val spinnerStyle: Int
        get() = Settings[SpinnerStyle].toIntOrNull() ?: 1

    @JvmStatic
    val metronomeSwitch: Int
        get() = Settings[MetronomeSwitch].toIntOrNull() ?: 1

    /**
     * Whether storyboards are shown, which a background too dark to see them disables.
     */
    @JvmStatic
    val isStoryboardEnabled: Boolean
        get() = backgroundBrightness > 0.02 && Settings[EnableStoryboard]

    /**
     * Whether to stay logged in, only possible in builds with online access.
     */
    @JvmStatic
    val isStayOnline: Boolean
        get() = Settings[StayOnline] && BuildType.hasOnlineAccess()

    /**
     * The name of the account, or "Guest" without one.
     */
    @JvmStatic
    val onlineUsername: String
        get() = Settings[OnlineUsername].ifEmpty { "Guest" }

    @JvmStatic
    var difficultyAlgorithm: DifficultyAlgorithm
        get() = if (Settings[DifficultyAlgorithmSetting] == "1") DifficultyAlgorithm.standard else DifficultyAlgorithm.droid
        set(value) {
            Settings[DifficultyAlgorithmSetting] = if (value == DifficultyAlgorithm.standard) "1" else "0"
        }

    @JvmStatic
    var beatmapLeaderboardScoringMode: BeatmapLeaderboardScoringMode
        get() = BeatmapLeaderboardScoringMode.parse(Settings[BeatmapLeaderboardScoringModeSetting].toIntOrNull() ?: 0)
        set(value) {
            Settings[BeatmapLeaderboardScoringModeSetting] = value.ordinal.toString()
        }

    /**
     * The colors of the combos when [UseCustomComboColors] is on.
     */
    @JvmStatic
    val comboColors: Array<Color4>
        get() = arrayOf(ComboColor1, ComboColor2, ComboColor3, ComboColor4).map {
            Color4("#" + (Settings[it] and 0xFFFFFF).toString(16).padStart(6, '0'))
        }.toTypedArray()

    /**
     * The identifier of this installation, created the first time it's needed.
     */
    @JvmStatic
    val installId: String
        get() = Settings[InstallId].ifEmpty {
            Uuid.random().toHexString().substring(0, 32).also { Settings[InstallId] = it }
        }

    //endregion


    /**
     * Prepares the settings on start, replacing what `Config.loadConfig` did: on the first start
     * with them, the preferences kept by the platform before are imported (see
     * [OsuGameHost.legacyPreferences]), then the language and the viewport follow
     * them.
     */
    @JvmStatic
    fun load() {
        // Only once: right after, the installation has its identifier.
        if (Settings.isNew && !Settings.contains(InstallId)) {
            importLegacyPreferences(OsuGameHost.current.legacyPreferences())
        }

        if (!Settings.contains(InstallId)) {
            // The folders of a new installation are kept, they'd move if the storage changed.
            Settings[CorePath] = GameFolders.corePath
            Settings[SkinTopPath] = SkinFolders.topPath
            Settings[SkinPath] = SkinFolders.path
            installId
        }

        GameResolution.measure()
        OsuGameHost.current.setAnnouncementsSubscribed(Settings[ReceiveAnnouncements])
        Languages.apply(Settings[Language])
    }

    /**
     * Stores the preferences the game had before keeping its settings in Verktex, by name.
     */
    @JvmStatic
    fun importLegacyPreferences(preferences: Map<String, Any?>) {
        if (preferences.isEmpty()) return

        Settings.putAll(preferences.mapNotNull { (name, value) -> value?.let { name to toJson(it) } }.toMap())

        // The backend chosen before is the engine's from the next start.
        if (Settings.contains(LegacyGraphicsBackend)) {
            Settings[VerktexSettings.GraphicsBackend] = Settings[LegacyGraphicsBackend]
        }

        Settings.flush()
        Logger.i("OsuSettings", "Imported ${preferences.size} preferences")
    }

    /**
     * [value] as JSON, a value of a preference or of a backup.
     */
    internal fun toJson(value: Any): JsonElement = when (value) {
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Collection<*> -> JsonArray(value.filterNotNull().map(::toJson))
        else -> JsonPrimitive(value.toString())
    }
}

