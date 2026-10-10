# Compose previews

Every screen and every component has a `@Preview`, so a change can be looked at without building and running the app. Keep it that way:

- **A preview lives in `commonMain`, in the same file as the composable it shows.** A preview of a `private` composable — the rows and cards inside a screen — can only be written there, and a parallel file per screen drifts as soon as the screen gains a parameter.
- **Every screen and component preview is wrapped in `AppPreview`** (`ui/Previews.kt`). It provides the theme, `LocalStrings` in one language and the window size classes, so a preview is drawn the way the app draws the screen.
- **A preview never touches a ViewModel, Koin or a repository.** It builds the screen's `UiState` from the fixtures in `ui/PreviewData.kt` and calls the stateless `XScreen(state, onEvent, …)` with `onEvent = {}` and empty navigation callbacks. A component preview sits on `PreviewBoard`, with `PreviewLabel` naming each variant.
- **Give each preview the size it is about** with `widthDp`/`heightDp`, and cover the layouts a screen has: phone (400x860), medium (700x900), desktop (1100x900), and compact height (800x420) for the screens that read `LocalHeightClass`. Those are the numbers `LocalWidthClass`/`LocalHeightClass` go by, so the size a preview asks for *is* the layout it shows.
- **Show the states a screen or component has**, one preview per state rather than one preview with parameters: enabled/disabled, checked/unchecked, passed/failed/in progress/not started, revealed/not revealed, with and without a value. A new state that changes what is drawn gets a preview that shows it.
- **Name the function `<What>Preview` and keep it `private`** to its file, with `@Preview` above the `@Composable`.
- **English is the language of a preview** unless the layout has to be checked against a longer translation — that is what `AppPreview(language = AppLanguage.Slovak)` is for.
- **Adding a composable means adding its preview in the same change.** A change inside a file that has no preview for what it touches adds one, too. Wording of the form "I'll add the preview later" is not accepted: the preview is part of the change.

Routes (`XRoute`, `ExamNavigation`) are not previewed: they resolve a ViewModel and a back stack, which a preview has none of — their screen is what the previews show. `App()` keeps its one whole-app preview as a smoke check.

Previews are ordinary `commonMain` sources: they compile on every target (Android, iOS, JS, Wasm), so nothing platform-only belongs in one, and `./gradlew :app:shared:compileAndroidMain` — plus any other target's compile — is what proves a preview is valid.
