# Screen sizes and languages

## Screen sizes

We support every screen size: phones and tablets in both orientations and desktop browsers via the web target. Every screen must work at all of them:

- Read the width class from `LocalWidthClass` (`ui/Adaptive.kt`: Compact < 600dp, Medium < 840dp, Expanded ≥ 840dp) instead of checking platforms.
- Phones in landscape are supported (orientation is not locked on Android or iOS). Check `LocalHeightClass` too: on Compact height (< 480dp), put things side by side and slim down the bars, as `PracticeScreen` does.
- Top-level navigation is a bottom `NavigationBar` on Compact and a `NavigationRail` on Medium/Expanded (see `App.kt`).
- Don't let content stretch: cap it with `ContentWidth` (Narrow for forms/settings, Reading for question text, Wide for multi-pane) and center it. Use extra width for extra panes or grid columns, not wider rows.
- Everything must be usable with a mouse and keyboard as well as touch (e.g. pagers need prev/next buttons, since mouse drags don't swipe).

## Languages

UI text is translated into English, Slovak, Hungarian and Czech. Never hardcode user-visible strings in composables; add them to `i18n/Strings.kt` for all four languages and read them via `strings`. Question content is not translated — it comes from the question bank in the bank's language.

Strings that describe the privacy policy or the terms of use have to agree with those documents; see [legal-documents.md](legal-documents.md).
