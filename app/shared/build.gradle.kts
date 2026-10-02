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
            implementation(libs.multiplatformSettings.noArg)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.aboutlibraries.compose.m3)
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
