package com.spiraclestudios.autoskola.data.analytics

/** Records what a test expected to be sent, and every consent it was handed. */
class RecordingAnalyticsDataSource : AnalyticsDataSource {

    /** What a test asserted was sent, and every consent the data source was handed. */
    val events = mutableListOf<AnalyticsEvent>()
    val properties = mutableListOf<Map<String, Any>>()
    val screens = mutableListOf<String>()

    /** Every consent [setConsent] was called with, in order. */
    val consents = mutableListOf<Pair<Boolean, Boolean>>()

    /**
     * What the sink reports as this device's identifier, for a test that is about the screen showing
     * it. Reported only once something has been allowed to leave, the way the real SDK reports it.
     */
    var identifier: String? = null

    /**
     * Whether an answer allowing something to leave has been given. Only [setConsent] sets it, which
     * is what lets a test pin that *reading* the identifier never starts anything.
     */
    var started: Boolean = false
        private set

    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) {
        events += event
        this.properties += properties
    }

    override fun screen(name: String) {
        screens += name
    }

    override fun deviceIdentifier(): String? = identifier?.takeIf { started }

    override fun setConsent(crashReporting: Boolean, analytics: Boolean) {
        consents += crashReporting to analytics
        if (crashReporting || analytics) started = true
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
