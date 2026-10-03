import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Applied further down, and only when the symbol-upload credentials are present.
    alias(libs.plugins.posthogAndroid) apply false
}

/**
 * PostHog's symbol upload is optional. The credentials it needs are a secret, so they live only
 * where release builds run — CI's environment, or a local `.env` that is gitignored — and a
 * developer is not expected to have them.
 *
 * With none of them the plugin is never applied, so a developer's release build minifies and skips
 * the upload rather than failing. The trade is that crash reports from such a build are
 * unreadable, which is what the message below says (see docs/error-reporting.md).
 */
val postHogDotenv: Map<String, String> = rootProject.file(".env")
    .takeIf { it.isFile }
    ?.readLines()
    ?.mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        trimmed.substringBefore("=").trim() to trimmed.substringAfter("=", "").trim()
    }
    ?.toMap()
    .orEmpty()

fun postHogCredential(name: String): String? {
    val fromEnvironment = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
    return fromEnvironment ?: postHogDotenv[name]?.takeIf { it.isNotBlank() }
}

/**
 * Whether `posthog-cli` is on PATH. The plugin shells out to it, and without it the upload task
 * fails the whole release build rather than skipping — so having the credentials is not enough.
 */
fun postHogCliIsInstalled(): Boolean {
    val path = providers.environmentVariable("PATH").orNull ?: return false
    val names = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("posthog-cli.cmd", "posthog-cli.exe", "posthog-cli")
    } else {
        listOf("posthog-cli")
    }
    return path.split(File.pathSeparator)
        .any { dir -> names.any { name -> File(dir, name).isFile } }
}

// Named so they cannot shadow the task properties of the same name inside configureEach below,
// which would read as a self-assignment and fail with a circular-evaluation error.
val symbolUploadApiKey = postHogCredential("POSTHOG_CLI_API_KEY")
val symbolUploadProjectId = postHogCredential("POSTHOG_CLI_PROJECT_ID")

if (symbolUploadApiKey != null && symbolUploadProjectId != null && postHogCliIsInstalled()) {
    apply(plugin = "com.posthog.android")
    tasks.withType<com.posthog.android.PostHogCliExecTask>().configureEach {
        postHogHost = postHogCredential("POSTHOG_CLI_HOST")
        postHogProjectId = symbolUploadProjectId
        postHogApiKey = symbolUploadApiKey
    }
} else {
    logger.lifecycle(
        "PostHog symbol upload is off. The release build is still minified, but crash reports " +
            "from it will not be symbolicated. To turn it on, this machine needs " +
            "POSTHOG_CLI_API_KEY and POSTHOG_CLI_PROJECT_ID in the environment or in .env, and " +
            "posthog-cli on PATH. See docs/error-reporting.md.",
    )
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

/** The application id a released build ships under. */
val releaseApplicationId = "com.spiraclestudios.autoskola"

/** The id a debug build ships under, so it installs next to a released app and keeps its own data. */
val debugApplicationIdSuffix = ".dev"
val debugApplicationId = releaseApplicationId + debugApplicationIdSuffix

/**
 * The AdMob app id, which the ads SDK reads out of the manifest.
 *
 * There are two, and they must not be swapped: Google's demo id belongs to no account and can be
 * used by anyone, while ours serves paid ads and reports against our account. A debug build showing
 * the real id bills an impression for a developer's own tap, which is a policy problem as well as a
 * waste; a release build showing the demo id serves nothing at all and shows up as no fill.
 *
 * The demo id is the default and a build type has to opt out of it, because that is the harmless
 * way round for a build type to forget.
 */
val demoAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val releaseAdMobAppId = "ca-app-pub-7362127401681337~8243476603"

android {
    namespace = "com.spiraclestudios.autoskola"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = releaseApplicationId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        // The version every Gradle-built target of this app reports (see the root build file).
        versionName = rootProject.version.toString()
        // The demo id here, and the real one in the release build type below.
        manifestPlaceholders["admobAppId"] = demoAdMobAppId
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
        getByName("debug") {
            applicationIdSuffix = debugApplicationIdSuffix
        }
        getByName("release") {
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
            // Release builds are minified, so the mapping file exists and PostHog's symbol upload
            // has something to send (docs/error-reporting.md). R8 rewrites the app's own frames,
            // which is why proguard-rules.pro exists.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    // The launcher class sits in this module's namespace, while the app id carries the debug suffix.
    commandLine(
        adbExecutable(),
        "shell",
        "am",
        "start",
        "-n",
        "$debugApplicationId/com.spiraclestudios.autoskola.MainActivity",
    )
}
