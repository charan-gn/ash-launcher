# AGENTS.md

Instructions for AI agents and humans working on this repo.

## What this is

`ash` — a terminal-inspired, minimal Android launcher. Fork of [Olauncher](https://github.com/tanujnotes/Olauncher) (GPL-3.0). Goal: Blloc-Ratio-level calm, Linux-terminal personality via ASCII art and monospace type, with horizontal pages that eventually hold notes, tasks, weather, and quick actions.

Read `README.md` for vision and roadmap status.

## Architecture (verified against the code — keep this accurate)

- **Single activity:** `app/src/main/java/app/olauncher/MainActivity.kt`. Declares the HOME intent filter. Hosts a `NavHostFragment`.
- **UI approach:** XML layouts + ViewBinding only. **No Compose in this codebase — do not introduce it without explicit instruction.**
- **Navigation:** `app/src/main/res/navigation/nav_graph.xml`
  - `mainFragment` → `ui/HomeFragment.kt` (home screen)
  - `appListFragment` → `ui/AppDrawerFragment.kt` (app drawer)
  - `settingsFragment` → `ui/SettingsFragment.kt`
- **State:** `MainViewModel` (app list, selected app, live events), `Prefs` (SharedPreferences wrapper in `data/Prefs.kt`), constants in `data/Constants.kt`.
- **Gestures:** `listener/OnSwipeTouchListener.kt` and `listener/ViewSwipeTouchListener.kt`. On home today: swipe up → app drawer, swipe down → notification shade, swipe left/right → user-configured app launch, long press → settings, double tap → lock screen.
- **Drawer:** `AppDrawerFragment` + `AppDrawerAdapter` + `SearchView` + `RecyclerView`; overscroll at top closes it.
- **Build:** minSdk 24, compile/target SDK 36, JDK 17, AGP/Gradle 8.9.1, Kotlin 2.1.20. ViewBinding enabled.

## Rules

1. **Small, buildable commits.** Run `./gradlew assembleDebug` before committing. One concern per commit.
2. **Follow the existing stack.** XML + ViewBinding + ViewPager2. Don't mix paradigms.
3. **Preserve Olauncher behavior** unless the task explicitly replaces it. Don't silently delete gestures or prefs.
4. **ASCII art conventions** (once art is added): store in `app/src/main/assets/ascii/*.txt`, never inline in Kotlin. Bundle the monospace font — never rely on system fallback. Provide narrow (≤32 col) and wide (≤48 col) variants of large pieces. Render with fixed line height, `letterSpacing = 0`. Test on a narrow phone width.
5. **License:** GPL-3.0. Keep `LICENSE` and upstream attribution. Do not add code that can't ship under GPL-3.0.
6. **Package identity:** complete the rebrand before release work. Debug builds currently use `app.olauncher.debug`.

## Roadmap state

- [x] Fork Olauncher base (full history, `upstream` remote kept for syncing)
- [ ] Rebrand package / app identity (in progress)
- [ ] 3-page horizontal pager + text page indicator
- [ ] Empty app drawer (swipe up; no app list yet by design)
- [ ] ASCII splash screen + mascot
- [ ] Command bar (app launch, calculator, quick note)
- [ ] Panels: notes, tasks, weather
- [ ] Theming pass

## Known gotchas

- **Gesture conflict:** horizontal left/right swipes currently launch user-configured apps. The pager intentionally takes over horizontal swipes. The left/right-launch prefs remain in place but become inert on the pager home; document this instead of ripping out the prefs.
- **App drawer is intentionally empty** until the pager milestone is done. Do not re-enable app listing without explicit instruction.
- **Home content replaced:** the original 8-pinned-apps home layout is being replaced by the pager. App launching remains available via the drawer later; don't assume the old home app slots exist.
- **fastlane/ metadata** upstream references `app.olauncher` package ids and Play Store listing. Do not republish it as `ash` without rewriting it.
- Olauncher Pro prompts and rate-app dialogs still reference upstream app ids; they need removal or repointing during theming/polish.
