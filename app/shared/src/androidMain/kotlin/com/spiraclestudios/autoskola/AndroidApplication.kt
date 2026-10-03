package com.spiraclestudios.autoskola

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

private var installedApplication: Application? = null

/** Whether the lifecycle callbacks below have been registered already. */
private var tracksActivities = false

/**
 * The activity in front, held weakly.
 *
 * A fullscreen ad is shown by an activity rather than from a context, and this module has no activity
 * of its own, so one is followed here. Weakly, because holding an activity means holding a whole
 * screen: this reference must never be what keeps a finished one alive.
 */
private var installedActivity: WeakReference<Activity>? = null

/**
 * Hands the shared module the application, and is called from the Android entry point before the
 * first frame. There is no other way to get one here: this module is a library with no activity of
 * its own, and it registers no `ContentProvider`.
 *
 * Until it is called — in a host test, or in a Compose preview rendered without a screen around it
 * — [applicationContext] answers `null`.
 *
 * It also starts following which activity is in front, which is the other thing here that needs the
 * entry point's help: what is in front is not something a library can ask for. Registering happens
 * once, because the entry point calls this again every time an activity is created.
 */
fun provideApplication(application: Application) {
    installedApplication = application
    if (!tracksActivities) {
        tracksActivities = true
        application.registerActivityLifecycleCallbacks(FrontActivity)
    }
}

/** The application, or `null` where one was never installed. Nothing here may assume it exists. */
fun applicationContext(): Application? = installedApplication

/**
 * The activity in front, or `null` when there is none.
 *
 * What shows a fullscreen ad, which is why it exists, and `null` is a state the caller has to answer
 * for: an ad cannot be shown from nowhere, so nothing is shown rather than something failing.
 */
fun currentActivity(): Activity? = installedActivity?.get()

/**
 * Follows the activity in front.
 *
 * Only resumed and paused matter. A paused activity is still alive but is no longer the screen the
 * user is looking at, and showing an ad from one of those is how an ad ends up behind the app.
 */
private object FrontActivity : Application.ActivityLifecycleCallbacks {

    override fun onActivityResumed(activity: Activity) {
        installedActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Only if it is still the one being held: a new activity may have been resumed already.
        if (installedActivity?.get() === activity) installedActivity = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (installedActivity?.get() === activity) installedActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
