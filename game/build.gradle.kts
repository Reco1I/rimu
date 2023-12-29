plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.reco1l.rimu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reco1l.rimu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/db-schemas")
}

dependencies {

    // BEGIN libGDX

    implementation("com.badlogicgames.gdx:gdx-backend-android:1.12.1")

    api("com.badlogicgames.gdx:gdx:1.12.1")
    api("io.github.libktx:ktx-app:1.12.1-rc1")
    api("io.github.libktx:ktx-assets:1.12.1-rc1")
    api("io.github.libktx:ktx-graphics:1.12.1-rc1")

    // END libGDX

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-serialization
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.10")

    // https://mvnrepository.com/artifact/androidx.room/room-runtime
    implementation("androidx.room:room-runtime:2.6.0")

    // https://mvnrepository.com/artifact/androidx.room/room-compiler
    ksp("androidx.room:room-compiler:2.6.0")

    // https://mvnrepository.com/artifact/androidx.room/room-ktx
    implementation("androidx.room:room-ktx:2.6.0")

    // https://mvnrepository.com/artifact/commons-codec/commons-codec
    implementation("commons-codec:commons-codec:1.16.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-serialization
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.10")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-properties
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.6.0")

    // https://github.com/BigBadaboom/androidsvg
    implementation("com.caverock:androidsvg-aar:1.4")

    // https://github.com/Reco1I/AndEngine
    implementation("com.github.Reco1I:AndEngine:1.0.4")

    // https://github.com/Reco1I/Basskt
    implementation("com.github.Reco1I:Basskt:1.0.2")

    // https://github.com/Reco1I/Toolkt
    implementation("com.github.Reco1I:Toolkt:1.0.3")
}
