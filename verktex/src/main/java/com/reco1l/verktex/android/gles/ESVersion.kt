package com.reco1l.verktex.android.gles

/**
 * The OpenGL ES versions a backend exists for.
 */
enum class ESVersion(
    /**
     * The `EGL_CONTEXT_CLIENT_VERSION` of a context of this version.
     */
    val clientVersion: Int,
) {
    ES3(3),
    ES2(2),
}
