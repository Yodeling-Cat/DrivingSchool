import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.app.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.leakcanary.android)
}

android {
    namespace = "uno.lux.autoskola"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uno.lux.autoskola"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // The shared code asks this module's BuildConfig whether it is running a debug build.
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

/** adb from the SDK in local.properties (or ANDROID_HOME), falling back to PATH. */
fun adbExecutable(): String {
    val properties = Properties()
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { properties.load(it) }
    val sdk = properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    val name = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
    return if (sdk == null) name else "${sdk.trim()}/platform-tools/$name"
}

// Builds, installs and launches the debug build on the connected device.
tasks.register<Exec>("runDebug") {
    group = "install"
    description = "Installs the Debug build and starts it on the connected device."
    dependsOn("installDebug")
    commandLine(adbExecutable(), "shell", "am", "start", "-n", "uno.lux.autoskola/.MainActivity")
}
