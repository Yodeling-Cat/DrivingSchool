# Architecture

## Layers

`:app:shared` has three layers. Each one only talks to the layer below it.

```
Screen (composable)  --UiEvent-->  ViewModel  --calls-->  Repository  --calls-->  DataSource
                     <--UiState--             <--Flow---              <--Flow---
```

**Screens** render state and send events. Each feature has two composables:
- `XScreen(state, onEvent, …navigation callbacks)` is stateless. It renders a `UiState` and reports user actions as `UiEvent`s. It never calls a ViewModel or repository.
- `XRoute(…)` gets the ViewModel with `koinViewModel()`, collects `uiState` with `collectAsStateWithLifecycle()`, and passes navigation callbacks through.

**ViewModels** (`XViewModel`, next to the screen in the same feature package) hold the screen's logic:
- Each one exposes `uiState: StateFlow<XUiState>` and one public `fun onEvent(event: XUiEvent)`. That's the only way screens talk to them.
- `XUiState` and `XUiEvent` are defined in the ViewModel's file. States are immutable data classes; use a sealed interface when a screen has distinct phases (`Loading`, `Ready`, `Closed`).
- Build `uiState` with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Compute the initial value from synchronous data when you can (for example preferences), so the first frame doesn't flash.
- Navigation that depends on the result of work goes through state: `HomeUiState.Ready.openAttemptId`, `ExamSelectionUiState.done`, `PracticeUiState.Closed`. The Route navigates and, if needed, sends an event to acknowledge it (`HomeUiEvent.AttemptOpened`). Pure navigation (back, "change exam") goes straight from the screen to a navigation callback.
- Don't resolve UI strings in ViewModels. Put the facts in state (`AttemptKind`, test number, `ExamLabel`) and let the screen format them with `strings`.

**Repositories** (`data/<area>/XRepository`) hold the rules and combine data sources. Examples: attempts are locked once closed, banks are cached, and the current exam is derived from preferences and banks. `ExamSession` holds the rules for one exam on top of `AttemptRepository`, reading the user's settings as they stand now.

**Data sources** (`data/<area>/XDataSource`) only store and load data. Each one is an interface with one implementation per backend:
- `SettingsAttemptDataSource` stores attempts in `Settings`.
- `ResourceQuestionBankDataSource` reads the bundled banks.
- A database or API would be another implementation. `PreferencesRepository` uses `Settings` directly, because `Settings` is already an interface.

**Navigation** uses Navigation 3 (`navigation/ExamNavigation.kt`):
- The back stack is a list of `@Serializable` `NavKey`s. Every new key must be registered in `navConfiguration`, so the back stack can be saved on iOS and web too.
- Each entry has its own ViewModel store, so a screen's ViewModel is cleared when the screen is popped. Pass arguments through the key, for example `PracticeKey(attemptId)` with `koinViewModel { parametersOf(attemptId) }`.
- Before navigation starts, `AppViewModel` picks the top-level destination: disclaimer, exam choice, loading or the exam.

**Dependency injection** uses Koin (`di/AppModule.kt`):
- Data sources and repositories are `single`s. ViewModels are `viewModel`s.
- `initKoin()` starts Koin once per process. The repositories must outlive activities, because retained ViewModels still use them.
- `AppModuleTest` checks that every ViewModel can be created, so register new ones there too.

**Images** are loaded with Coil 3. The data source turns a question's image path into a URI (`BundledContent.uri`), the ViewModel puts it in state, and the screen draws it with `rememberAsyncImagePainter`. Coil caches the images and scales them down to the size they're shown at.

**Testing ViewModels:**
- Extend `ViewModelTest` (`commonTest/.../testing/`). It swaps `Dispatchers.Main` for an immediate test dispatcher.
- Create ViewModels lazily, after that swap.
- Use `observe(viewModel.uiState)` to keep the state subscribed like a screen would.
- `TestRepositories` wires the real repositories over `MapSettings` and a fake bank.

## Data access

Attempts are stored in `Settings` today. We write their API as if they lived in a database, so we can move to one (probably SQLDelight, see `plans/sqldelight.md`) by adding an `AttemptDataSource` implementation:

- **Reads return a `Flow`** that emits again when the data changes (`AttemptRepository.statistics`, `ExamSession.progress`, `ExamSession.openAttempt`, `ExamRepository.current`). Don't add synchronous getters, even though `Settings` could answer them instantly.
- **Writes are `suspend` functions.** `AttemptRepository` runs them in `NonCancellable` and one at a time, so a write finishes even when the ViewModel that started it is cleared.
- **Show a loading state until the first value arrives.** Don't assume a flow emits immediately.
- **Tests use `runTest`** and read flows with `.first()`.

This applies to user progress: attempts and anything we derive from them. `PreferencesRepository` stays synchronous (a `StateFlow` with plain setters) on every platform, because it's a few small values the app needs to draw its first frame.

## Statistics

Every test run is a `TestAttempt` (`:core`, `com.spiraclestudios.autoskola.stats`). It records the bank id and version, the license group, the questions in order, each answer with its timestamp, and the start, finish or discard times. `AttemptRepository` (`data/attempts/`) saves an attempt on every answer, so a test survives the app being killed and can be resumed.

Everything else is derived from attempts by `Statistics`: latest result per test, the resumable attempt, per-question stats and mistakes. Don't store derived numbers separately. Answers count towards statistics only once they're final: either the attempt is finished, or the user is checking answers immediately right now.

An attempt stores no app-wide setting. Whether answers are checked as they're picked decides what the practice screen reveals and locks, and whether a running attempt's answers count yet, so `ExamSession` and the practice screen read it from `PreferencesRepository` every time they need it: a saved test that is continued goes by the settings the user has now, not the ones it was started under.

Statistics, results and mistakes are scoped to one bank version; see [question-banks.md](question-banks.md).
