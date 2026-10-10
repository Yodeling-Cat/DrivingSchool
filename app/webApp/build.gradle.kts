import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * The country a web build is for: `sk` unless `-Pcountry=cz` says otherwise. A page is built for
 * one country, as each Android flavor is (see docs/countries.md).
 */
val country = providers.gradleProperty("country").getOrElse("sk")

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
            implementation(project(":app:content:$country"))

            implementation(libs.compose.ui)
        }
    }
}
