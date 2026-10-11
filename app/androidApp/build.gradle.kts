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

    // The store screenshots (`tools/screenshots/`). The only instrumented test, and not part of any
    // test run: see `storeScreenshotAnnotation` below.
    androidTestImplementation(libs.androidx.testRunner)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(project.dependencies.platform(libs.koin.bom))
    androidTestImplementation(libs.koin.core)
    androidTestImplementation(libs.multiplatformSettings)
    androidTestImplementation(libs.kotlinx.coroutines.core)
}

/**
 * Marks the store-screenshot test, which `connectedAndroidTest` leaves out: it takes minutes, needs
 * an emulator per screen size, and checks nothing. `tools/screenshots/take-screenshots` runs it by
 * class with `am instrument`, which does not pass this filter.
 */
val storeScreenshotAnnotation = "com.spiraclestudios.autoskola.screenshots.StoreScreenshot"

/**
 * The application id a released build ships under. It is the Slovak app's, which Play has held since
 * before there were countries; the Czech app is a separate listing under the same id with `.cz` after
 * it (see the `cz` flavor below).
 */
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

/**
 * The AdMob app id of each country's released app. An id belongs to one app, and the Czech app has
 * none yet: its release build carries the demo id, and its ad units are empty too
 * (`AdUnits.android.kt`), so it shows no ads rather than another app's.
 */
val releaseAdMobAppIds = mapOf("sk" to "ca-app-pub-7362127401681337~8243476603")

android {
    namespace = "com.spiraclestudios.autoskola"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = releaseApplicationId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Play already holds 45 for this package, from the listing that predates this rewrite, and
        // an upload has to be higher than anything it has seen. Raise it for every upload.
        versionCode = 47
        // The version every Gradle-built target of this app reports (see the root build file).
        versionName = rootProject.version.toString()
        // The demo id here, and the real one in the release build type below.
        manifestPlaceholders["admobAppId"] = demoAdMobAppId

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = storeScreenshotAnnotation
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // The shared code asks this module's BuildConfig whether it is running a debug build, and which
    // country it is built for (`FLAVOR`).
    buildFeatures {
        buildConfig = true
    }
    // One app per country, each bundling only its own questions, pictures and videos.
    flavorDimensions += "country"
    productFlavors {
        create("sk") {
            dimension = "country"
            // What Android Studio selects on a fresh sync, rather than the first flavor alphabetically.
            isDefault = true
        }
        create("cz") {
            dimension = "country"
            applicationIdSuffix = ".cz"
        }
    }
    // The upload key, which Play checks before it re-signs the app with its own. It comes from the
    // environment so the keystore and its passwords never sit in the repo: the release workflow
    // sets them from secrets, and a developer exports them in the shell. Without all of them the
    // release build is simply left unsigned, as it is for anyone who has no key.
    val uploadKeystore = System.getenv("AUTOSKOLA_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    val uploadStorePassword = System.getenv("AUTOSKOLA_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    val uploadKeyAlias = System.getenv("AUTOSKOLA_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "upload"
    val uploadKeyPassword = System.getenv("AUTOSKOLA_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: uploadStorePassword
    val canSignRelease = uploadKeystore != null && uploadStorePassword != null
    if (canSignRelease) {
        signingConfigs {
            create("release") {
                storeFile = file(uploadKeystore)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = debugApplicationIdSuffix
        }
        getByName("release") {
            if (canSignRelease) signingConfig = signingConfigs.getByName("release")
            // Release builds are minified, so the mapping file exists and PostHog's symbol upload
            // has something to send (docs/error-reporting.md). R8 rewrites the app's own frames,
            // which is why proguard-rules.pro exists.
            isMinifyEnabled = true
            // Drops resources nothing references, which only works once R8 has removed the code
            // that used them.
            isShrinkResources = true
            // Native libraries come from dependencies (the ads SDK, for one) and arrive stripped,
            // so Play warns that the bundle has no debug symbols. The symbol table is what is left
            // to extract from them, and it makes native crash reports show function names.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
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

// Each country's app bundles its own content and no other's (see docs/countries.md). Declared
// after the `android` block, which is what creates the flavors' configurations.
dependencies {
    "skImplementation"(projects.app.content.sk)
    "czImplementation"(projects.app.content.cz)
}

// The real AdMob app id goes into a country's release build alone: debug builds, and a country with
// no AdMob app yet, keep the demo id that `defaultConfig` sets.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        releaseAdMobAppIds[variant.flavorName]?.let { variant.manifestPlaceholders.put("admobAppId", it) }
    }
}
