plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.aboutLibraries) apply false
}

// The app's own version, in the one place a Gradle build can stamp into what it builds: the
// Android install (`app/androidApp/build.gradle.kts`) and the web bundle
// (`app/shared/build.gradle.kts`). iOS takes its version from the Xcode project's
// MARKETING_VERSION, which nothing here can reach (see `AppVersion.ios.kt`).
version = "1.2"

// Style checks for every module: `./gradlew ktlintCheck`, or `ktlintFormat` to fix what it can.
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint.get())
    }
}