package com.spiraclestudios.autoskola

import kotlinx.browser.document

actual fun setWindowTitle(title: String) {
    document.title = title
}
