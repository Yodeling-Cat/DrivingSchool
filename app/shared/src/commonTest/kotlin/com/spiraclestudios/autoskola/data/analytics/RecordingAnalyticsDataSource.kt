package com.spiraclestudios.autoskola.data.analytics

/** Records what a test expected to be sent, and every consent it was handed. */
class RecordingAnalyticsDataSource : AnalyticsDataSource {

    val events = mutableListOf<AnalyticsEvent>()
    val properties = mutableListOf<Map<String, Any>>()
    val screens = mutableListOf<String>()

    /** Every value [setEnabled] was called with, in order. */
    val enableCalls = mutableListOf<Boolean>()

    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) {
        events += event
        this.properties += properties
    }

    override fun screen(name: String) {
        screens += name
    }

    override fun setEnabled(enabled: Boolean) {
        enableCalls += enabled
    }
}
