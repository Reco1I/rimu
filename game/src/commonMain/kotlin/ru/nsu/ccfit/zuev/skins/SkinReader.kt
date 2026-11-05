package ru.nsu.ccfit.zuev.skins

abstract class SkinReader {
    fun loadSkin() {
        OsuSkin.get().reset()
        loadSkinBase()
    }

    protected abstract fun loadSkinBase()

    protected abstract fun loadComboColorSetting()

    protected abstract fun loadSlider()

    protected abstract fun loadUtils()

    protected abstract fun loadLayout()

    protected abstract fun loadColor()

    protected abstract fun loadCursor()

    protected abstract fun loadFonts()

    protected abstract fun loadTheme()

    protected fun putLayout(name: String, layout: SkinLayout) {
        OsuSkin.get().layoutData[name] = layout
    }
}
