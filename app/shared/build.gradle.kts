import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.aboutLibraries)
}

aboutLibraries {
    library {
        // One entry per library rather than one per platform artifact: without this the list would
        // hold `collection-jvm` next to `collection-wasm-js` and name each of them twice.
        mergePlatformArtifacts = true
    }
    export {
        // Committed, like the question banks: the plugin is not wired into the build, so
        // `./gradlew :app:shared:exportLibraryDefinitions` is what refreshes it.
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}

/**
 * The country an iOS framework is built for, since Xcode has no Android-style flavors: `sk` unless
 * `-Pcountry=cz` says otherwise (see AGENTS.md, "Countries").
 */
val country = providers.gradleProperty("country").getOrElse("sk")
require(country in setOf("sk", "cz")) { "country must be sk or cz, not $country" }

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.spiraclestudios.autoskola.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            // The ads SDK is Android only. iOS and the web targets have no implementation of their
            // own yet - they answer `NoOpAdDataSource` - which is `plans/ads.md` step 13.
            implementation(libs.play.services.ads)
            implementation(libs.play.services.ump)
            implementation(libs.play.billing)
            // Google Play's in-app review sheet, which the home screen's rating card opens.
            implementation(libs.play.review)
            // The player for the Czech bank's videos (`ui/VideoSurface.android.kt`).
            implementation(libs.androidx.media3.exoplayer)
        }
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiBackhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.navigation3.ui)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)
            implementation(libs.coil.compose)
            implementation(libs.posthog.kmp)
            implementation(libs.multiplatformSettings.noArg)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.aboutlibraries.compose.m3)
        }
        iosMain.dependencies {
            // The framework is the iOS app, so it bundles the one country it is built for.
            implementation(project(":app:content:$country"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatformSettings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            // On web, bundled resources are files on the server, so Coil needs HTTP to load them.
            implementation(libs.coil.networkKtor)
            implementation(libs.ktor.clientJs)
        }
        wasmJsMain.dependencies {
            implementation(libs.coil.networkKtor)
            implementation(libs.ktor.clientJs)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

/**
 * The version this build is, written into a generated source the web targets compile: a page is
 * served from a file rather than installed, so nothing on it would otherwise know which build it
 * is. It is the root project's version, which is also what an Android install reports.
 */
val generateWebAppVersion by tasks.registering {
    val version = rootProject.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/webAppVersion")
    inputs.property("appVersion", version)
    outputs.dir(outputDir)
    doLast {
        val source = outputDir.get().file("com/spiraclestudios/autoskola/WEB_APP_VERSION.kt").asFile
        source.parentFile.mkdirs()
        source.writeText(
            buildString {
                appendLine("package com.spiraclestudios.autoskola")
                appendLine()
                appendLine("/** The version this web build is, written by the Gradle build that made it. */")
                appendLine("internal const val WEB_APP_VERSION: String = \"$version\"")
            },
        )
    }
}

// Wired once the project has been evaluated: the Kotlin plugin builds the source set that holds
// `src/webMain` while it is finishing, so asking for one by name earlier is asking for a name that
// does not exist yet. It is the group source set both web targets compile, which is where a web
// build's own code lives (beside NavigationState.web.kt) - and a task provider as a source
// directory is what makes the compilation wait for the file.
gradle.projectsEvaluated {
    kotlin.sourceSets.getByName("webMain").kotlin.srcDir(generateWebAppVersion)
}
