# AGENTS.md

Instructions for AI agents and humans working on this repo.

## What this is

`ash` — a terminal-inspired, minimal Android launcher. Fork of [Olauncher](https://github.com/tanujnotes/Olauncher) (GPL-3.0). Goal: Blloc-Ratio-level calm, Linux-terminal personality via ASCII art and monospace type, with horizontal pages that eventually hold notes, tasks, weather, and quick actions.

Read `README.md` for vision and roadmap status.

## Architecture (verified against the code — keep this accurate)

- **Single activity:** `app/src/main/java/app/ash/launcher/MainActivity.kt`. Declares the HOME intent filter. Hosts a `NavHostFragment`.
- **UI approach:** XML layouts + ViewBinding only. **No Compose in this codebase — do not introduce it without explicit instruction.**
- **Navigation:** `app/src/main/res/navigation/nav_graph.xml`
  - `mainFragment` → `ui/HomeFragment.kt` (home screen; `ViewPager2` with 2–5 pages)
  - `appListFragment` → `ui/AppDrawerFragment.kt` (app drawer; intentionally empty)
  - `settingsFragment` → `ui/SettingsFragment.kt`
- **Home pager (TUI shell):** `ui/HomePagerAdapter.kt` + `res/layout/item_home_page.xml`. Pages are plain `RecyclerView` items (not fragments) rendered by the reusable `ui/widget/AsciiPanelView` — it measures the view in character cells and draws a full box-drawn panel (`┌─ title ─┐`) around its content, so borders always fit the screen. `HomeFragment` builds a tap-able tab bar (`home notes tasks feed more` from `res/values/strings.xml#home_pages`), inverts the active tab, and renders a bottom status line (`HH:mm · page`) plus a hint line. Page count and start page come from `Prefs.pageCount` (2–5) and `Prefs.startPage` (1-based), edited in settings. Pages are placeholders; add real page content to the panel body.
- **Fonts:** bundled JetBrains Mono at `res/font/jetbrains_mono_regular.ttf` (OFL license in `licenses/OFL-JetBrainsMono.txt`). It is the **Nerd Fonts–patched** build (`JetBrainsMonoNerdFontMono-Regular`), required so box-drawing and `●○` glyphs render natively — plain JetBrains Mono lacks them and the fallback font breaks ASCII layouts. New ASCII/monospace UI must use it; never rely on system monospace fallback.
- **State:** `MainViewModel` (app list, selected app, live events), `Prefs` (SharedPreferences wrapper in `data/Prefs.kt`), constants in `data/Constants.kt`.
- **Settings:** `ui/SettingsFragment.kt` + `res/layout/fragment_settings.xml` are a hand-built terminal panel (ASCII section headers, mono labels, values on the right), not a PreferenceScreen. Live rows: set default, source, app info, pages, start page, ASCII splash, status bar, theme (long-press for system), text scale. No layout-land variant — the single scrolling layout is used in both orientations.
- **Splash:** `activity_main.xml` has a full-screen `splash` TextView showing `splash_art`; `MainActivity.onCreate` fades it out when `Prefs.asciiSplash` is on and this is a cold start.
- **Gestures:** `listener/OnSwipeTouchListener.kt` (attached to each pager page in `HomePagerAdapter`) and `listener/ViewSwipeTouchListener.kt` (empty-drawer overlay). On the pager home: horizontal swipe → page change (ViewPager2), swipe up → empty app drawer, swipe down → notification shade, long press → settings. The old left/right app-launch swipes and double-tap lock are no longer wired on home.
- **Drawer:** `AppDrawerFragment` + `AppDrawerAdapter` + `SearchView` + `RecyclerView`; overscroll at top closes it. With `DRAWER_EMPTY = true`, a full-screen `AsciiPanelView` titled `apps` replaces the list.
- **Build:** minSdk 24, compile/target SDK 36, JDK 17, AGP/Gradle 8.9.1, Kotlin 2.1.20. ViewBinding enabled.

## Rules

1. **Small, buildable commits.** Run `./gradlew assembleDebug` before committing. One concern per commit.
2. **Follow the existing stack.** XML + ViewBinding + ViewPager2. Don't mix paradigms.
3. **Preserve Olauncher behavior** unless the task explicitly replaces it. Don't silently delete gestures or prefs.
4. **ASCII art conventions** (once art is added): store in `app/src/main/assets/ascii/*.txt`, never inline in Kotlin. Bundle the monospace font — never rely on system fallback. Provide narrow (≤32 col) and wide (≤48 col) variants of large pieces. Render with fixed line height, `letterSpacing = 0`. Test on a narrow phone width.
5. **License:** GPL-3.0. Keep `LICENSE` and upstream attribution. Do not add code that can't ship under GPL-3.0.
6. **Package identity:** `app.ash.launcher` (debug: `app.ash.launcher.debug`). Keep it consistent in manifests, nav graphs, accessibility config, and release metadata.

## Roadmap state

- [x] Fork Olauncher base (full history, `upstream` remote kept for syncing)
- [x] Rebrand package / app identity (`app.ash.launcher`)
- [x] TUI home shell: boxed pages, tab bar, status/hint lines (2–5 pages configurable)
- [x] Empty app drawer (swipe up; no app list yet by design)
- [x] ASCII terminal settings panel
- [x] ASCII splash screen (toggleable; no mascot yet)
- [ ] Command bar (app launch, calculator, quick note)
- [ ] Panels: notes, tasks, weather
- [ ] Theming pass

## Known gotchas

- **Gesture conflict resolved by pager:** horizontal swipes now page; the old swipe-left/right app-launch prefs remain in `Prefs` but are inert on the pager home. Don't silently delete them; decide the gesture mapping before rewiring.
- **Font is Nerd-patched.** Do not replace `res/font/jetbrains_mono_regular.ttf` with plain JetBrains Mono: box-drawing glyphs (`┌─┐│└┘`) and `●○` disappear and fallback metrics clip the ASCII frames (confirmed on device).
- **App drawer is intentionally empty.** Controlled by `DRAWER_EMPTY = true` in `AppDrawerFragment`; the layout keeps the search/recycler views hidden behind `emptyState`. Swipe down or tap closes it. Do not re-enable app listing without explicit instruction.
- **Home content replaced:** the original clock + 8-pinned-apps home layout is gone; `HomeFragment` was rewritten. Don't resurrect `homeApp1..8` or the old swipe listeners; add real content via pager pages instead.
- **fastlane/ metadata** was removed (it referenced upstream package ids). Add new publishing metadata only when a release pipeline exists.
- **Upstream promo system is gone.** `checkForMessages`, user-state review/share/pro/wallpaper dialogs, and their settings entry points were removed. Don't reintroduce review-begging or upstream links; keep `Constants.Dialog` unused for now.
- **Dead Olauncher settings were removed** (pinned-app count, date/time, alignment, auto-keyboard, daily wallpaper, bold font, swipe-apps rows, double-tap lock, hidden apps, share/rate/twitter). Their prefs and helper code remain in the data layer but have no UI; don't resurrect rows without an ash feature behind them.
- Old strings referencing "Olauncher" still exist in translated resources; clean up in the theming/polish pass.
