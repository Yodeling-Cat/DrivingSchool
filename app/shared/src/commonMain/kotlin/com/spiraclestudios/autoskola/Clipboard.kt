package com.spiraclestudios.autoskola

/**
 * Puts [text] on the system clipboard: a phone's own service, or the browser's, which the mail app
 * the user writes in can paste from.
 *
 * Nothing is answered, because there is nothing to answer with: the write on the web is
 * asynchronous, and the only browser that refuses outright is one with no clipboard at all - a
 * page that is not a secure context, which is not a way this app is served. The copy button says
 * the text was copied, and the same text is drawn in a selection under it for the reader who would
 * rather take it by hand.
 */
expect fun copyToClipboard(text: String)
