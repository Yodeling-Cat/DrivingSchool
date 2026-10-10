package com.spiraclestudios.autoskola

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.spiraclestudios.autoskola.content.bundledContent
import com.spiraclestudios.autoskola.provideApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app draws one palette and it is a light one (see `NajAutoskolaTheme`), so the system
        // bars are drawn light with it: dark icons, and a dark gesture line. Left to itself the
        // window works their direction out from the *system's* dark mode, which on a phone in dark
        // mode puts white icons and a white line on our paper, where there is nothing to see them
        // against. Both scrims stay transparent, so it is the app's own paper the bars sit on.
        //
        // A dark palette would be this call's problem too: the bars have to follow the palette on
        // screen, not the system's.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // PostHog's Android context wraps the Application, and the shared module has no activity
        // or provider of its own to find one with, so the entry point hands it over.
        provideApplication(application)

        setContent {
            App(bundledContent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(bundledContent)
}
