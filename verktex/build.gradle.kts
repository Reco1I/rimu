plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

// The coordinates applications depend on, com.reco1l.verktex:<module>, which a build including
// this one substitutes with the projects.
subprojects {
    group = "com.reco1l.verktex"
}
