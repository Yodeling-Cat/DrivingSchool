package uno.lux.autoskola.navigation

import com.russhwolf.settings.Settings

/** iOS comes back to the screen it was on from its own settings; see [StoredNavigationState]. */
actual fun navigationState(settings: Settings): NavigationState = StoredNavigationState(settings)
