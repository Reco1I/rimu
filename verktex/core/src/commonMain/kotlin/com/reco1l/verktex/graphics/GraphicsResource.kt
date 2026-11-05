package com.reco1l.verktex.graphics

abstract class GraphicsResource {

    var state = State.Unloaded
        protected set


    /**
     * Called by the renderer when the context is lost. This will force the resource to be loaded on
     * the next render call.
     */
    fun markContextLost() {
        if (state == State.Unloaded) return
        onContextLost()
    }


    fun bind() {
        if (state == State.Unloaded) {
            load()
        }
        onBind()
    }

    fun load() {
        if (state == State.Loaded) return

        onLoad()
        state = State.Loaded
    }

    fun unload() {
        if (state == State.Unloaded) return

        onUnload()
        state = State.Unloaded
    }


    open fun dispose() {
        unload()
    }


    /**
     * Called when the graphics context is lost. i.e. when [markContextLost] is called.
     */
    open fun onContextLost() {
        state = State.Unloaded
    }


    /**
     * Called when the resource is bound to the GPU. This method should contain the logic for
     * binding the resource to the GPU.
     */
    protected abstract fun onBind()

    /**
     * Called when the resource is loaded into the GPU. This method should contain the logic for
     * loading the resource into the GPU.
     */
    protected abstract fun onLoad()

    /**
     * Called when the resource is unloaded from the GPU. This method should contain the logic for
     * unloading the resource from the GPU.
     */
    protected abstract fun onUnload()


    /**
     * Represents the state of the graphics resource, indicating whether it is currently loaded
     * into the GPU or not.
     */
    enum class State {
        Loaded,
        Unloaded
    }
}