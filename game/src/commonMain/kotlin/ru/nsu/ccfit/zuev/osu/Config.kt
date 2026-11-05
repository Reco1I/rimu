package ru.nsu.ccfit.zuev.osu

import com.osudroid.OsuPlatform
import com.osudroid.utils.SystemFileSystem
import com.reco1l.verktex.Logger
import com.reco1l.verktex.data.Color4
import com.russhwolf.settings.Settings
import kotlin.math.max
import kotlin.math.min
import kotlin.uuid.Uuid
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.helper.Debug
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object Config {
    private var corePath: String = ""
    private var defaultCorePath: String = ""
    private var beatmapPath: String = ""
    private var cachePath: String = ""
    private var skinPath: String = ""
    private var skinTopPath: String = ""
    private var scorePath: String = ""
    private var onlineUsername: String = ""
    private var onlinePassword: String = ""
    private var onlineDeviceID: String = ""

    private var DELETE_OSZ = false
    private var SCAN_DOWNLOAD = false
    private var deleteUnimportedBeatmaps = false
    private var showFirstApproachCircle = false
    private var comboburst = false
    private var useCustomSkins = false
    private var useCustomSounds = false
    private var corovans = false
    private var showFPS = false
    private var animateFollowCircle = false
    private var animateComboText = false
    private var snakingInSliders = false
    private var snakingOutSliders = false
    private var playMusicPreview = false
    private var showCursor = false
    private var enableExtension = false
    private var loadAvatar = false
    private var stayOnline = false
    private var burstEffects = false
    private var hitLighting = false
    private var useParticles = false
    private var useCustomComboColors = false
    private var forceRomanized = false
    private var fixFrameOffset = false
    private var removeSliderLock = false
    private var displayScoreStatistics = false
    private var hideReplayMarquee = false
    private var hideInGameUI = false
    private var receiveAnnouncements = false
    private var enableStoryboard = false
    private var safeBeatmapBg = false
    private var useNightcoreOnMultiplayer = false
    private var videoEnabled = false
    private var submitScoreOnMultiplayer = false
    private var preferModAcronymInMultiplayer = false
    private var keepBackgroundAspectRatio = false
    private var noChangeDimInBreaks = false
    private var dimHitObjects = false
    private var forceMaxRefreshRate = false
    private var shiftPitchInRateChange = false

    private var RES_WIDTH = 0
    private var RES_HEIGHT = 0
    private var spinnerStyle = 0
    private var metronomeSwitch = 0
    private var backButtonPressTime = 0

    private var soundVolume = 0f
    private var bgmVolume = 0f
    private var offset = 0f
    private var backgroundBrightness = 0f
    private var playfieldSize = 0f
    private var playfieldHorizontalPosition = 0f
    private var playfieldVerticalPosition = 0f
    private var cursorSize = 0f

    private var skins: MutableMap<String, String>? = null

    private var comboColors: Array<Color4> = emptyArray()

    @JvmField
    val SENSITIVE_KEYS: Set<String> = setOf(
        "installID",
        "onlineUsername",
        "onlinePassword",
        "droidStarRatingVersion",
        "standardStarRatingVersion",
        "version"
    )

    @JvmField
    val PREFERENCE_BOUNDS: Map<String, IntArray> = mapOf(
        "bgmvolume" to intArrayOf(0, 100),
        "soundvolume" to intArrayOf(0, 100),
        "bgbrightness" to intArrayOf(0, 100),
        "offset" to intArrayOf(-750, 750),
        "cursorSize" to intArrayOf(25, 300),
        "playfieldSize" to intArrayOf(50, 100),
        "playfieldHorizontalPosition" to intArrayOf(0, 100),
        "playfieldVerticalPosition" to intArrayOf(0, 100),
        "back_button_press_time" to intArrayOf(0, 300),
        "seekBarVibrateIntensity" to intArrayOf(1, 255),
        "room_max_players" to intArrayOf(2, 16)
    )

    /**
     * The numeric preferences stored as floats. Everything else stays an integer.
     */
    @JvmField
    val FLOAT_PREFERENCES: Set<String> = setOf("bgmvolume", "soundvolume", "bgbrightness")

    /**
     * The shared preferences of the application.
     */
    private lateinit var sharedPreferences: Settings


    @JvmStatic
    fun loadConfig() {
        sharedPreferences = OsuPlatform.implementation.preferences

        val prefs = sharedPreferences
        // graphics
        useCustomSkins = prefs.getBoolean("skin", false)
        useCustomSounds = prefs.getBoolean("beatmapSounds", true)
        comboburst = prefs.getBoolean("comboburst", false)
        corovans = prefs.getBoolean("images", false)
        showFPS = prefs.getBoolean("fps", true)
        spinnerStyle = prefs.getString("spinnerstyle", "1").toInt()
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false)
        metronomeSwitch = prefs.getString("metronomeswitch", "1").toInt()
        enableStoryboard = prefs.getBoolean("enableStoryboard", false)
        videoEnabled = prefs.getBoolean("enableVideo", false)
        keepBackgroundAspectRatio = prefs.getBoolean("keepBackgroundAspectRatio", false)
        noChangeDimInBreaks = prefs.getBoolean("noChangeDimInBreaks", false)
        dimHitObjects = prefs.getBoolean("dimHitObjects", true)
        forceMaxRefreshRate = prefs.getBoolean("forceMaxRefreshRate", false)

        measureDisplaySize()
        setPlayfieldSize(prefs.getInt("playfieldSize", 100) / 100f)
        playfieldHorizontalPosition = prefs.getInt("playfieldHorizontalPosition", 50) / 100f
        playfieldVerticalPosition = prefs.getInt("playfieldVerticalPosition", 50) / 100f

        animateFollowCircle = prefs.getBoolean("animateFollowCircle", true)
        animateComboText = prefs.getBoolean("animateComboText", true)
        snakingInSliders = prefs.getBoolean("snakingInSliders", true)
        snakingOutSliders = prefs.getBoolean("snakingOutSliders", true)

        try {
            offset = prefs.getInt("offset", 0).toFloat()
            backgroundBrightness = getNumber("bgbrightness", 25f) / 100f
            soundVolume = getNumber("soundvolume", 100f) / 100f
            bgmVolume = getNumber("bgmvolume", 100f) / 100f
            cursorSize = prefs.getInt("cursorSize", 50) / 100f
        } catch (e: RuntimeException) { // use valid integer since this makes the game crash on android m
            prefs.putInt("offset", 0)
            prefs.putFloat("bgbrightness", 25f)
            prefs.putFloat("soundvolume", 100f)
            prefs.putFloat("bgmvolume", 100f)
            prefs.putInt("cursorSize", 50)
            loadConfig()
            return
        }

        //advanced
        defaultCorePath = OsuPlatform.implementation.defaultCorePath
        corePath = prefs.getString("corePath", defaultCorePath)
        if (corePath.length == 0) {
            corePath = defaultCorePath
        }
        if (corePath[corePath.length - 1] != '/') {
            corePath += "/"
        }
        scorePath = corePath + "Scores/"

        skinPath = prefs.getString("skinPath", corePath + "Skin/")
        if (skinPath.length == 0) {
            skinPath = corePath + "Skin/"
        }
        if (skinPath[skinPath.length - 1] != '/') {
            skinPath += "/"
        }

        skinTopPath = prefs.getString("skinTopPath", skinPath)
        if (skinTopPath.length == 0) {
            skinTopPath = skinPath
        }
        if (skinTopPath[skinTopPath.length - 1] != '/') {
            skinTopPath += "/"
        }

        enableExtension = false// prefs.getBoolean("enableExtension", false);
        cachePath = OsuPlatform.implementation.cachePath
        burstEffects = prefs.getBoolean("bursts", burstEffects)
        hitLighting = prefs.getBoolean("hitlighting", hitLighting)
        useParticles = prefs.getBoolean("particles", useParticles)
        useCustomComboColors = prefs.getBoolean("useCustomColors", useCustomComboColors)
        comboColors = Array(4) { i ->
            Color4("#" + (prefs.getInt("combo" + (i + 1), 0xff000000.toInt()) and 0xFFFFFF).toString(16).padStart(6, '0'))
        }

        // beatmaps
        DELETE_OSZ = prefs.getBoolean("deleteosz", true)
        SCAN_DOWNLOAD = prefs.getBoolean("scandownload", false)
        deleteUnimportedBeatmaps = prefs.getBoolean("deleteUnimportedBeatmaps", false)
        forceRomanized = prefs.getBoolean("forceromanized", false)
        beatmapPath = prefs.getString("directory", corePath + "Songs/")
        if (beatmapPath.length == 0) {
            beatmapPath = corePath + "Songs/"
        }
        if (beatmapPath[beatmapPath.length - 1] != '/') {
            beatmapPath += "/"
        }

        // other
        playMusicPreview = prefs.getBoolean("musicpreview", true)
        showCursor = prefs.getBoolean("showcursor", false)
        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true)
        removeSliderLock = prefs.getBoolean("removeSliderLock", false)
        displayScoreStatistics = prefs.getBoolean("displayScoreStatistics", false)
        hideReplayMarquee = prefs.getBoolean("hideReplayMarquee", false)
        hideInGameUI = prefs.getBoolean("hideInGameUI", false)
        receiveAnnouncements = prefs.getBoolean("receiveAnnouncements", true)
        safeBeatmapBg = prefs.getBoolean("safebeatmapbg", false)
        shiftPitchInRateChange = prefs.getBoolean("shiftPitchInRateChange", false)
        backButtonPressTime = getInt("back_button_press_time", 300)

        // Multiplayer
        useNightcoreOnMultiplayer = prefs.getBoolean("player_nightcore", false)
        submitScoreOnMultiplayer = prefs.getBoolean("player_submitScore", true)
        preferModAcronymInMultiplayer = prefs.getBoolean("player_preferModAcronym", false)

        OsuPlatform.implementation.setAnnouncementsSubscribed(receiveAnnouncements)

        //Init
        val installID = prefs.getStringOrNull("installID")
        if (installID == null) {
            onlineDeviceID = Uuid.random().toHexString().substring(0, 32)
            prefs.putString("installID", onlineDeviceID)
            prefs.putString("corePath", corePath)
            prefs.putString("skinTopPath", skinTopPath)
            prefs.putString("skinPath", skinPath)
        } else {
            onlineDeviceID = installID
        }

        loadOnlineConfig()
    }

    @JvmStatic
    fun loadOnlineConfig() {
        val prefs = sharedPreferences

        onlineUsername = prefs.getString("onlineUsername", "")
        onlinePassword = prefs.getString("onlinePassword", "")
        stayOnline = prefs.getBoolean("stayOnline", false)
        loadAvatar = prefs.getBoolean("loadAvatar", false)
    }

    @JvmStatic
    fun measureDisplaySize() {
        val displayWidth = OsuPlatform.implementation.displayWidth
        val displayHeight = OsuPlatform.implementation.displayHeight

        val width = max(displayWidth, displayHeight)
        val height = min(displayWidth, displayHeight)

        if (getBoolean("use_legacy_resolution_policy", true)) {
            // Tries to emulate the original behavior, the game was designed for 1280x720
            // resolution, so we try to approximate the scale factor.
            val ratio = 1280f / width

            RES_WIDTH = (width * ratio).toInt()
            RES_HEIGHT = (height * ratio).toInt()
        } else {
            RES_WIDTH = width
            RES_HEIGHT = height
        }

        Logger.i("Config", "Viewport size: " + RES_WIDTH + "x" + RES_HEIGHT + " Display size: " + width + "x" + height)
    }

    @JvmStatic
    fun isEnableStoryboard(): Boolean {
        return backgroundBrightness > 0.02 && enableStoryboard
    }

    @JvmStatic
    fun setEnableStoryboard(enableStoryboard: Boolean) {
        Config.enableStoryboard = enableStoryboard
    }

    @JvmStatic
    fun isFixFrameOffset(): Boolean {
        return fixFrameOffset
    }

    @JvmStatic
    fun isRemoveSliderLock(): Boolean {
        return removeSliderLock
    }

    @JvmStatic
    fun isDisplayScoreStatistics(): Boolean {
        return displayScoreStatistics
    }

    @JvmStatic
    fun getDifficultyAlgorithm(): DifficultyAlgorithm {
        return if (getString("difficultyAlgorithm", "0") == "1")
                DifficultyAlgorithm.standard
                else DifficultyAlgorithm.droid
    }

    @JvmStatic
    fun isEnableExtension(): Boolean {
        return enableExtension
    }

    @JvmStatic
    fun setEnableExtension(enableExtension: Boolean) {
        Config.enableExtension = enableExtension
    }

    @JvmStatic
    fun isShowFPS(): Boolean {
        return showFPS
    }

    @JvmStatic
    fun setShowFPS(showFPS: Boolean) {
        Config.showFPS = showFPS
    }

    @JvmStatic
    fun isShowScoreboard(): Boolean {
        return getBoolean("showscoreboard", true)
    }

    @JvmStatic
    fun setShowScoreboard(showScoreboard: Boolean) {
        setBoolean("showscoreboard", showScoreboard)
    }

    @JvmStatic
    fun isCorovans(): Boolean {
        return corovans
    }

    @JvmStatic
    fun setCorovans(corovans: Boolean) {
        Config.corovans = corovans
    }

    @JvmStatic
    fun getSoundVolume(): Float {
        return soundVolume
    }

    @JvmStatic
    fun setSoundVolume(volume: Float) {
        Config.soundVolume = volume
    }

    @JvmStatic
    fun getBgmVolume(): Float {
        return bgmVolume
    }

    @JvmStatic
    fun setBgmVolume(bgmVolume: Float) {
        Config.bgmVolume = bgmVolume
    }

    @JvmStatic
    fun getOffset(): Float {
        return offset
    }

    @JvmStatic
    fun setOffset(offset: Float) {
        Config.offset = offset
    }

    @JvmStatic
    fun getCorePath(): String {
        return corePath
    }

    @JvmStatic
    fun setCorePath(corePath: String) {
        Config.corePath = corePath
    }

    @JvmStatic
    fun getBeatmapPath(): String {
        return beatmapPath
    }

    @JvmStatic
    fun setBeatmapPath(path: String) {
        Config.beatmapPath = path
    }

    @JvmStatic
    fun getRES_WIDTH(): Int {
        return RES_WIDTH
    }

    @JvmStatic
    fun getRES_HEIGHT(): Int {
        return RES_HEIGHT
    }

    @JvmStatic
    fun isDELETE_OSZ(): Boolean {
        return DELETE_OSZ
    }

    @JvmStatic
    fun setDELETE_OSZ(dELETE_OSZ: Boolean) {
        Config.DELETE_OSZ = dELETE_OSZ
    }

    @JvmStatic
    fun isSCAN_DOWNLOAD(): Boolean {
        return SCAN_DOWNLOAD
    }

    @JvmStatic
    fun setSCAN_DOWNLOAD(sCAN_DOWNLOAD: Boolean) {
        Config.SCAN_DOWNLOAD = sCAN_DOWNLOAD
    }

    @JvmStatic
    fun isDeleteUnimportedBeatmaps(): Boolean {
        return deleteUnimportedBeatmaps
    }

    @JvmStatic
    fun setDeleteUnimportedBeatmaps(deleteUnimportedBeatmaps: Boolean) {
        Config.deleteUnimportedBeatmaps = deleteUnimportedBeatmaps
    }

    @JvmStatic
    fun isUseCustomSkins(): Boolean {
        return useCustomSkins
    }

    @JvmStatic
    fun setUseCustomSkins(useCustomSkins: Boolean) {
        Config.useCustomSkins = useCustomSkins
    }

    @JvmStatic
    fun isUseCustomSounds(): Boolean {
        return useCustomSounds
    }

    @JvmStatic
    fun setUseCustomSounds(useCustomSounds: Boolean) {
        Config.useCustomSounds = useCustomSounds
    }

    @JvmStatic
    fun getTextureQuality(): Int {
        return 1
    }

    @JvmStatic
    fun getBackgroundBrightness(): Float {
        return backgroundBrightness
    }

    @JvmStatic
    fun setBackgroundBrightness(backgroundBrightness: Float) {
        Config.backgroundBrightness = backgroundBrightness
    }

    @JvmStatic
    fun isAnimateFollowCircle(): Boolean {
        return animateFollowCircle
    }

    @JvmStatic
    fun isAnimateComboText(): Boolean {
        return animateComboText
    }

    @JvmStatic
    fun isSnakingInSliders(): Boolean {
        return snakingInSliders
    }

    @JvmStatic
    fun isSnakingOutSliders(): Boolean {
        return snakingOutSliders
    }

    @JvmStatic
    fun isPlayMusicPreview(): Boolean {
        return playMusicPreview
    }

    @JvmStatic
    fun setPlayMusicPreview(playMusicPreview: Boolean) {
        Config.playMusicPreview = playMusicPreview
    }

    @JvmStatic
    fun isShowCursor(): Boolean {
        return showCursor
    }

    @JvmStatic
    fun setShowCursor(showCursor: Boolean) {
        Config.showCursor = showCursor
    }

    @JvmStatic
    fun getOnlineUsername(): String {
        return if (onlineUsername.isNotEmpty()) onlineUsername else "Guest"
    }

    @JvmStatic
    fun setOnlineUsername(onlineUsername: String) {
        Config.onlineUsername = onlineUsername
    }

    @JvmStatic
    fun getOnlinePassword(): String {
        return onlinePassword
    }

    @JvmStatic
    fun setOnlinePassword(onlinePassword: String) {
        Config.onlinePassword = onlinePassword
    }

    @JvmStatic
    fun isStayOnline(): Boolean {
        return stayOnline && BuildType.hasOnlineAccess()
    }

    @JvmStatic
    fun setStayOnline(stayOnline: Boolean) {
        Config.stayOnline = stayOnline
    }

    @JvmStatic
    fun getBeatmapLeaderboardScoringMode(): BeatmapLeaderboardScoringMode {
        return BeatmapLeaderboardScoringMode.parse(getString("beatmapLeaderboardScoringMode", "0").toInt())
    }

    @JvmStatic
    fun setBeatmapLeaderboardScoringMode(beatmapLeaderboardScoringMode: BeatmapLeaderboardScoringMode) {
        setString("beatmapLeaderboardScoringMode", beatmapLeaderboardScoringMode.ordinal.toString())
    }

    @JvmStatic
    fun getLoadAvatar(): Boolean {
        return loadAvatar
    }

    @JvmStatic
    fun setLoadAvatar(loadAvatar: Boolean) {
        Config.loadAvatar = loadAvatar
    }

    @JvmStatic
    fun getOnlineDeviceID(): String {
        return onlineDeviceID
    }

    @JvmStatic
    fun getCachePath(): String {
        return cachePath
    }

    @JvmStatic
    fun setCachePath(cachePath: String) {
        Config.cachePath = cachePath
    }

    @JvmStatic
    fun isBurstEffects(): Boolean {
        return burstEffects
    }

    @JvmStatic
    fun setBurstEffects(burstEffects: Boolean) {
        Config.burstEffects = burstEffects
    }

    @JvmStatic
    fun isHitLighting(): Boolean {
        return hitLighting
    }

    @JvmStatic
    fun setHitLighting(hitLighting: Boolean) {
        Config.hitLighting = hitLighting
    }

    @JvmStatic
    fun isUseParticles(): Boolean {
        return useParticles
    }

    @JvmStatic
    fun setUseParticles(useParticles: Boolean) {
        Config.useParticles = useParticles
    }

    @JvmStatic
    fun getSkinPath(): String {
        return skinPath
    }

    @JvmStatic
    fun setSkinPath(skinPath: String) {
        Config.skinPath = skinPath
    }

    @JvmStatic
    fun getSkinTopPath(): String {
        return skinTopPath
    }

    @JvmStatic
    fun setSkinTopPath(skinTopPath: String) {
        Config.skinTopPath = skinTopPath
    }

    @JvmStatic
    fun getScorePath(): String {
        return scorePath
    }

    @JvmStatic
    fun setScorePath(scorePath: String) {
        Config.scorePath = scorePath
    }

    @JvmStatic
    fun isUseCustomComboColors(): Boolean {
        return useCustomComboColors
    }

    @JvmStatic
    fun setUseCustomComboColors(useCustomComboColors: Boolean) {
        Config.useCustomComboColors = useCustomComboColors
    }

    @JvmStatic
    fun getComboColors(): Array<Color4> {
        return comboColors
    }

    @JvmStatic
    fun getSpinnerStyle(): Int {
        return spinnerStyle
    }

    @JvmStatic
    fun setSpinnerStyle(spinnerStyle: Int) {
        Config.spinnerStyle = spinnerStyle
    }

    @JvmStatic
    fun isShowFirstApproachCircle(): Boolean {
        return showFirstApproachCircle
    }

    @JvmStatic
    fun setShowFirstApproachCircle(showFirstApproachCircle: Boolean) {
        Config.showFirstApproachCircle = showFirstApproachCircle
    }

    @JvmStatic
    fun getMetronomeSwitch(): Int {
        return metronomeSwitch
    }

    @JvmStatic
    fun setMetronomeSwitch(metronomeSwitch: Int) {
        Config.metronomeSwitch = metronomeSwitch
    }

    @JvmStatic
    fun isComboburst(): Boolean {
        return comboburst
    }

    @JvmStatic
    fun setComboburst(comboburst: Boolean) {
        Config.comboburst = comboburst
    }

    @JvmStatic
    fun isForceRomanized(): Boolean {
        return forceRomanized
    }

    @JvmStatic
    fun setForceRomanized(forceRomanized: Boolean) {
        Config.forceRomanized = forceRomanized
    }

    @JvmStatic
    fun getCursorSize(): Float {
        return cursorSize
    }

    @JvmStatic
    fun setCursorSize(cursorSize: Float) {
        Config.cursorSize = cursorSize
    }

    @JvmStatic
    fun getPlayfieldSize(): Float {
        return playfieldSize
    }

    @JvmStatic
    fun setPlayfieldSize(playfieldSize: Float) {
        Config.playfieldSize = playfieldSize
    }

    @JvmStatic
    fun getPlayfieldHorizontalPosition(): Float {
        return playfieldHorizontalPosition
    }

    @JvmStatic
    fun setPlayfieldHorizontalPosition(playfieldHorizontalPosition: Float) {
        Config.playfieldHorizontalPosition = playfieldHorizontalPosition
    }

    @JvmStatic
    fun getPlayfieldVerticalPosition(): Float {
        return playfieldVerticalPosition
    }

    @JvmStatic
    fun setPlayfieldVerticalPosition(playfieldVerticalPosition: Float) {
        Config.playfieldVerticalPosition = playfieldVerticalPosition
    }

    @JvmStatic
    fun isHideReplayMarquee(): Boolean {
        return hideReplayMarquee
    }

    @JvmStatic
    fun setHideReplayMarquee(hideReplayMarquee: Boolean) {
        Config.hideReplayMarquee = hideReplayMarquee
    }

    @JvmStatic
    fun isHideInGameUI(): Boolean {
        return hideInGameUI
    }

    @JvmStatic
    fun setHideInGameUI(hideInGameUI: Boolean) {
        Config.hideInGameUI = hideInGameUI
    }

    @JvmStatic
    fun isReceiveAnnouncements(): Boolean {
        return receiveAnnouncements
    }

    @JvmStatic
    fun setReceiveAnnouncements(receiveAnnouncements: Boolean) {
        Config.receiveAnnouncements = receiveAnnouncements
    }

    @JvmStatic
    fun isSafeBeatmapBg(): Boolean {
        return safeBeatmapBg
    }

    @JvmStatic
    fun setSafeBeatmapBg(safeBeatmapBg: Boolean) {
        Config.safeBeatmapBg = safeBeatmapBg
    }

    @JvmStatic
    fun isTrianglesAnimation(): Boolean {
        return false
    }

    @JvmStatic
    fun getDefaultCorePath(): String {
        return defaultCorePath
    }

    @JvmStatic
    fun loadSkins() {
        val topPath = skinTopPath.toPath()
        val folders = (SystemFileSystem.listOrNull(topPath) ?: emptyList()).filter {
            SystemFileSystem.metadataOrNull(it)?.isDirectory == true && !it.name.startsWith(".")
        }
        skins = HashMap()
        for (folder in folders) {
            skins!![folder.name] = folder.toString()
            Debug.i("skins: " + folder.name + " - " + folder.toString())
        }
    }

    @JvmStatic
    fun getSkins(): MutableMap<String, String>? {
        return skins
    }

    @JvmStatic
    fun addSkin(name: String, path: String) {
        if (skins == null) skins = HashMap()
        skins!![name] = path
    }

    @JvmStatic
    fun isUseNightcoreOnMultiplayer(): Boolean {
        return useNightcoreOnMultiplayer
    }

    @JvmStatic
    fun setUseNightcoreOnMultiplayer(value: Boolean) {
        Config.useNightcoreOnMultiplayer = value
    }

    @JvmStatic
    fun isVideoEnabled(): Boolean {
        return videoEnabled
    }

    @JvmStatic
    fun setVideoEnabled(value: Boolean) {
        Config.videoEnabled = value
    }

    @JvmStatic
    fun isSubmitScoreOnMultiplayer(): Boolean {
        return submitScoreOnMultiplayer
    }

    @JvmStatic
    fun setSubmitScoreOnMultiplayer(submitScoreOnMultiplayer: Boolean) {
        Config.submitScoreOnMultiplayer = submitScoreOnMultiplayer
    }

    @JvmStatic
    fun isPreferModAcronymInMultiplayer(): Boolean {
        return preferModAcronymInMultiplayer
    }

    @JvmStatic
    fun setPreferModAcronymInMultiplayer(preferModAcronymInMultiplayer: Boolean) {
        Config.preferModAcronymInMultiplayer = preferModAcronymInMultiplayer
    }

    @JvmStatic
    fun isKeepBackgroundAspectRatio(): Boolean {
        return keepBackgroundAspectRatio
    }

    @JvmStatic
    fun isNoChangeDimInBreaks(): Boolean {
        return noChangeDimInBreaks
    }

    @JvmStatic
    fun isDimHitObjects(): Boolean {
        return dimHitObjects
    }

    @JvmStatic
    fun isForceMaxRefreshRate(): Boolean {
        return forceMaxRefreshRate
    }

    @JvmStatic
    fun isShiftPitchInRateChange(): Boolean {
        return shiftPitchInRateChange
    }

    @JvmStatic
    fun isDisplayPlayfieldBorder(): Boolean {
        return getBoolean("displayPlayfieldBorder", false)
    }

    @JvmStatic
    fun getBackButtonPressTime(): Int {
        return backButtonPressTime
    }

    @JvmStatic
    fun isPreferNoVideoDownloads(): Boolean {
        return getBoolean("preferNoVideoDownloads", false)
    }

    @JvmStatic
    fun isHighPrecisionInput(): Boolean {
        return getBoolean("highPrecisionInput", false)
    }

    // Shared Preferences
    // It's preferred to use these methods to access shared preferences instead of adding new fields to this class.
    // If the option is expected to be accessed frequently consider storing it locally as a field where it's needed.

    @JvmStatic
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        try {
            return sharedPreferences.getBoolean(key, defaultValue)
        } catch (e: ClassCastException) {
            Debug.e("Config: preference \"" + key + "\" has an unexpected type, using default", e)
            return defaultValue
        }
    }

    @JvmStatic
    fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.putBoolean(key, value)
    }

    @JvmStatic
    fun getInt(key: String, defaultValue: Int): Int {
        try {
            return sharedPreferences.getInt(key, defaultValue)
        } catch (e: ClassCastException) {
            Debug.e("Config: preference \"" + key + "\" has an unexpected type, using default", e)
            return defaultValue
        }
    }

    @JvmStatic
    fun setInt(key: String, value: Int) {
        sharedPreferences.putInt(key, value)
    }

    @JvmStatic
    fun getLong(key: String, defaultValue: Long): Long {
        try {
            return sharedPreferences.getLong(key, defaultValue)
        } catch (e: ClassCastException) {
            Debug.e("Config: preference \"" + key + "\" has an unexpected type, using default", e)
            return defaultValue
        }
    }

    @JvmStatic
    fun setLong(key: String, value: Long) {
        sharedPreferences.putLong(key, value)
    }

    @JvmStatic
    fun getString(key: String, defaultValue: String): String {
        try {
            return sharedPreferences.getString(key, defaultValue)
        } catch (e: ClassCastException) {
            Debug.e("Config: preference \"" + key + "\" has an unexpected type, using default", e)
            return defaultValue
        }
    }

    @JvmStatic
    fun setString(key: String, value: String) {
        sharedPreferences.putString(key, value)
    }

    /**
     * Gets a numeric preference no matter whether it was stored as an integer or as a float, so the
     * preferences in [FLOAT_PREFERENCES] keep the value they had before moving to floats.
     */
    @JvmStatic
    fun getNumber(key: String, defaultValue: Float): Float {
        return try { sharedPreferences.getFloatOrNull(key) } catch (e: ClassCastException) { null }
            ?: try { sharedPreferences.getIntOrNull(key)?.toFloat() } catch (e: ClassCastException) { null }
            ?: defaultValue
    }

    @JvmStatic
    fun getFloat(key: String, defaultValue: Float): Float {
        try {
            return sharedPreferences.getFloat(key, defaultValue)
        } catch (e: ClassCastException) {
            Debug.e("Config: preference \"" + key + "\" has an unexpected type, using default", e)
            return defaultValue
        }
    }

    @JvmStatic
    fun setFloat(key: String, value: Float) {
        sharedPreferences.putFloat(key, value)
    }
}
