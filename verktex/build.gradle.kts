import java.util.Properties

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

    ndkVersion = "22.1.7171670"

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    // The Vulkan backend, see docs/vulkan-backend.md.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
    implementation("androidx.core:core-ktx:1.13.1")

    implementation(project(":LibBASS"))

    // Shader translation and uniform layouts, shared with the build that compiles the Vulkan shaders.
    implementation(project(":verktex-shadertools"))

    //implementation("com.google.android.material:material:1.14.0")
    testImplementation("junit:junit:4.13.2")
    //androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //androidTestImplementation("androidx.test.ext:junit:1.1.5")

}


// Debug builds can package the Vulkan validation layer, which the backend enables when it's present.
// The NDK's own (1.2.158) crashes in vkCreateInstance on recent Android versions, so a current build
// from https://github.com/KhronosGroup/Vulkan-ValidationLayers/releases (android-binaries-*.zip) is
// used instead: pass its extracted folder, with one folder per ABI, as -Pverktex.validationLayers=<dir>
// or set verktex.validationLayers in local.properties.
abstract class CopyValidationLayers : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val layersDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val files: FileSystemOperations

    @TaskAction
    fun copy() {
        files.sync {
            from(layersDirectory)
            include("**/libVkLayer_khronos_validation.so")
            into(outputDirectory)
        }
    }
}

val validationLayers: String? = providers.gradleProperty("verktex.validationLayers").orNull
    ?: rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use(::load) }.getProperty("verktex.validationLayers")
    }

if (validationLayers != null) {
    val copyValidationLayers = tasks.register<CopyValidationLayers>("copyValidationLayers") {
        layersDirectory.set(file(validationLayers))
        outputDirectory.set(layout.buildDirectory.dir("generated/validationLayers"))
    }

    androidComponents {
        onVariants(selector().withBuildType("debug")) { variant ->
            variant.sources.jniLibs?.addGeneratedSourceDirectory(copyValidationLayers, CopyValidationLayers::outputDirectory)
        }
    }
}
