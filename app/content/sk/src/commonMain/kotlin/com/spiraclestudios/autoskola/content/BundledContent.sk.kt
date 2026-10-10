package com.spiraclestudios.autoskola.content

import com.spiraclestudios.autoskola.content.sk.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The Slovak content this module bundles. Every country's module names it the same, so an app's
 * entry point reads whichever country its build includes, and a build includes exactly one.
 */
@OptIn(ExperimentalResourceApi::class)
val bundledContent: BundledContent = object : BundledContent {
    override val country = "SK"

    override suspend fun readBytes(path: String): ByteArray = Res.readBytes(path)

    override fun uri(path: String): String = Res.getUri(path)
}
