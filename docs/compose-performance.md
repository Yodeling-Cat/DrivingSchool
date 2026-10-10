# Compose performance

Strong skipping is on, so every composable can skip. It recomposes anyway when a parameter changes even though nothing it draws did. To avoid that:

- **Read fast-changing values where they are drawn.** Pass anything that changes every second or every frame (the practice clock, the question sheet's drag, scroll offsets) as a lambda (`() -> T`). Read it in the composable that draws it, in `Modifier.layout`, or in `graphicsLayer { }`/`drawBehind { }`, never high up in a screen. When only a threshold matters, wrap the check in `derivedStateOf` (`cover() < 1f`).
- **Pass composables the fields they draw, not the whole `UiState`.** A child or slot lambda that takes or captures `state` recomposes on every emission: every keystroke on `FeedbackScreen`, every clock tick on `PracticeScreen`. Read the fields out first.
- **Keep instances stable across emissions.** Collections in a `UiState`, and classes from `:core`, are compared by identity. A ViewModel that rebuilds an equal list or map on every emission recomposes everything that takes it, so hand back the previous instance while the contents are equal (`PracticeViewModel.shownAnswers`). Cover this with `assertSame` in the ViewModel's test.
- **`remember` anything that costs something to build:** derived lists, `AnnotatedString`s, draw modifiers (`paperBackground`) and theme objects. A static composition local (`LocalNajType`, `LocalNajColors`) must get the same instance every time, or everything under it recomposes.
- **Give lazy items a `key`, plus a `contentType` when a list mixes kinds of items**, so a row that scrolls off is reused for another row of the same kind.
- **Play nothing on a page that is not on screen.** A pager composes the pages beside the current one ahead of time, effects and all, so a video or an animation there starts while nobody sees it. Pass the page an `active` flag (`page == pagerState.settledPage`) and play only while it is set, as `QuestionMedia` does.

When unsure, generate the compiler report by adding `composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_reports") }` to `app/shared/build.gradle.kts` for one build. On Windows the report lands in NTFS streams of a file named `NajAutoskola_app`, because the module's name contains a colon. Read them with `Get-Content -Stream`.
