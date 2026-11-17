# AGENTS.md

Instructions for AI agents and humans working on this repo.

## What this is

`ash` — a terminal-inspired, minimal Android launcher. Fork of [Olauncher](https://github.com/tanujnotes/Olauncher) (GPL-3.0). Goal: Blloc-Ratio-level calm, Linux-terminal personality via ASCII art and monospace type, with horizontal pages that eventually hold notes, tasks, weather, and quick actions.

Read `README.md` for vision and roadmap status.

## Architecture (verified against the code — keep this accurate)

- **Single activity:** `app/src/main/java/app/ash/launcher/MainActivity.kt`. Declares the HOME intent filter. Hosts a `NavHostFragment`.
- **UI approach:** XML layouts + ViewBinding only. **No Compose in this codebase — do not introduce it without explicit instruction.**
- **Navigation:** `app/src/main/res/navigation/nav_graph.xml`
  - `mainFragment` → `ui/HomeFragment.kt` (home screen; now a 3-page `ViewPager2`)
  - `appListFragment` → `ui/AppDrawerFragment.kt` (app drawer; intentionally empty)
  - `settingsFragment` → `ui/SettingsFragment.kt`
- **Home pager:** `ui/HomePagerAdapter.kt` + `res/layout/item_home_page.xml`. Pages are plain `RecyclerView` items (not fragments). Page labels are ASCII box art generated in `HomePagerAdapter.pageArt()`; indicator `[●○○]` is set by `HomeFragment.updateIndicator()`. Pages are placeholders; add real page content here.
- **Fonts:** bundled JetBrains Mono at `res/font/jetbrains_mono_regular.ttf` (OFL license in `licenses/OFL-JetBrainsMono.txt`). New ASCII/monospace UI must use it; never rely on system monospace fallback.
- **State:** `MainViewModel` (app list, selected app, live events), `Prefs` (SharedPreferences wrapper in `data/Prefs.kt`), constants in `data/Constants.kt`.
- **Gestures:** `listener/OnSwipeTouchListener.kt` (attached to each pager page in `HomePagerAdapter`) and `listener/ViewSwipeTouchListener.kt` (empty-drawer overlay). On the pager home: horizontal swipe → page change (ViewPager2), swipe up → empty app drawer, swipe down → notification shade, long press → settings. The old left/right app-launch swipes and double-tap lock are no longer wired on home.
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
- [x] Rebrand package / app identity (`app.ash.launcher`)
- [x] 3-page horizontal pager + text page indicator
- [x] Empty app drawer (swipe up; no app list yet by design)
- [ ] ASCII splash screen + mascot
- [ ] Command bar (app launch, calculator, quick note)
- [ ] Panels: notes, tasks, weather
- [ ] Theming pass

## Known gotchas

- **Gesture conflict resolved by pager:** horizontal swipes now page; the old swipe-left/right app-launch prefs remain in `Prefs` but are inert on the pager home. Don't silently delete them; decide the gesture mapping before rewiring.
- **App drawer is intentionally empty.** Controlled by `DRAWER_EMPTY = true` in `AppDrawerFragment`; the layout keeps the search/recycler views hidden behind `emptyState`. Swipe down or tap closes it. Do not re-enable app listing without explicit instruction.
- **Home content replaced:** the original clock + 8-pinned-apps home layout is gone; `HomeFragment` was rewritten. Don't resurrect `homeApp1..8` or the old swipe listeners; add real content via pager pages instead.
- **fastlane/ metadata** was removed (it referenced upstream package ids). Add new publishing metadata only when a release pipeline exists.
- Upstream promo dialogs (`MainActivity.checkForMessages`: review/share/pro/wallpaper) still run and point at upstream links. Remove them during polish; don't wire new features to them.
- Old strings referencing "Olauncher" still exist in translated resources and settings copy; clean up in the theming/polish pass.
