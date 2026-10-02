package com.spiraclestudios.autoskola

import platform.UIKit.UIPasteboard

/** The system pasteboard, which the mail app the user writes in can paste from. */
actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}
