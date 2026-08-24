plugins {
    id("com.android.library")
    id("com.android.built-in-kotlin")
}

kotlin {
   compilerOptions {
       freeCompilerArgs.add("-Xexplicit-backing-fields")
   }
}

android {
    namespace = "com.reco1l.verktex"
    compileSdk {
        version = release(34)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.1")

    // Android specific
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.19.0")

    implementation(project(":LibBASS"))

    //implementation("com.google.android.material:material:1.14.0")
    //testImplementation("junit:junit:4.13.2")
    //androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //androidTestImplementation("androidx.test.ext:junit:1.1.5")

}