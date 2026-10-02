package com.spiraclestudios.autoskola

/**
 * What the device says about itself, in one line, for the block a feedback report can carry - the
 * phone's maker and model, and the system it runs - or `null` where the platform has nothing to
 * report.
 *
 * Each target answers for itself, the same way [appVersion] and [isDebugBuild] do. It is the
 * device and the system only: nothing that identifies the user is asked for anywhere in the app,
 * and a report is read by a person who wants to know what the phone was doing, not who held it.
 */
expect fun deviceDescription(): String?
