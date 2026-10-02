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

    /**
     * Forgets everything recorded, for a test that checks one phase of a flow at a time. All four
     * at once: clearing `events` and leaving `properties` behind is how a later assertion ends up
     * reading the previous phase's values.
     */
    fun clear() {
        events.clear()
        properties.clear()
        screens.clear()
        consents.clear()
    }
}
