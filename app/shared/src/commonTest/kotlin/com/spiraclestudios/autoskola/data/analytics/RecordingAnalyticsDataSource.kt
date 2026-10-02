package com.spiraclestudios.autoskola.data.analytics

/** Records what a test expected to be sent, and every consent it was handed. */
class RecordingAnalyticsDataSource : AnalyticsDataSource {

    /** What a test asserted was sent, and every consent the data source was handed. */
    val events = mutableListOf<AnalyticsEvent>()
    val properties = mutableListOf<Map<String, Any>>()
    val screens = mutableListOf<String>()

    /** Every consent [setConsent] was called with, in order. */
    val consents = mutableListOf<Pair<Boolean, Boolean>>()

    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) {
        events += event
        this.properties += properties
    }

    override fun screen(name: String) {
        screens += name
    }

    override fun setConsent(crashReporting: Boolean, analytics: Boolean) {
        consents += crashReporting to analytics
    }
}
