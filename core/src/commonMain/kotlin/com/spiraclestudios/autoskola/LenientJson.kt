package com.spiraclestudios.autoskola

import kotlinx.serialization.json.Json

/**
 * The JSON every stored or bundled file is read with: a key it does not know is skipped, so a file
 * written by a newer version of the app or the importer still reads.
 */
val LenientJson = Json { ignoreUnknownKeys = true }
