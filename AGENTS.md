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
- **Home pager (TUI shell):** `ui/HomePagerAdapter.kt` + `res/layout/item_home_page.xml`. Pages are plain `RecyclerView` items (not fragments) rendered by the reusable `ui/widget/AsciiPanelView` — it measures the view in character cells, shrinks the text to fit the longest line, and draws a full box-drawn panel (`┌─ title ─┐`) around its content, so borders always fit the screen. Panel chrome is colored with theme attrs: dim borders, accent title, text content, and an accent page counter (`└─ 1/5 ─┘`). `HomeFragment` builds a tap-able tab bar (`home notes tasks music more` from `res/values/strings.xml#home_pages`) that marks the active tab with accent brackets (`[ home ]`) while inactive tabs are dim, and renders a status line (`HH:mm · page` plus an accent `[█████░░░░░] 52%` battery gauge) and hint line. Page count and start page come from `Prefs.pageCount` (2–5) and `Prefs.startPage` (1-based), edited in settings. Home renders a tty-clock-style block clock (`ui/widget/AsciiClock.kt`, fixed 5-row font, blinking colon), date, and a dim blinking `~ $ _` prompt; notes/tasks/more are placeholder pages filled with figlet word art from `assets/ascii/*.txt` via `helper/AsciiAssets.kt`; music is live.
- **Music page:** `player/MusicPlayer.kt` is owned by `MainViewModel` (activity scope, so playback survives page/fragment navigation). It queries `MediaStore` on `Dispatchers.IO`, plays via `MediaPlayer` with audio focus and `ACTION_AUDIO_BECOMING_NOISY` handling, and exposes a `Snapshot` polled by `HomeFragment.updateMusicPanel()`. Panel tap zones: left third → previous/restart, center → play/pause, right third → next. There is **no foreground service or media notification** — playback dies with the launcher process; add a service before promising background playback.
- **Palette:** the dark default is ash-and-ember (bg `#0F0F0F`, text `#D8D4CC`, dim `#6B6860`, accent `#E8A33D`), defined in `res/values/colors.xml` (`ash*`) and applied in `values-night/styles.xml`; light theme keeps the Olauncher black-on-white palette plus a darker amber accent. UI code must use `?attr/accentColor`, `?attr/dimColor`, and the existing `primary*` attrs — never hardcode hexes.
- **Fonts:** bundled JetBrains Mono at `res/font/jetbrains_mono_regular.ttf` (OFL license in `licenses/OFL-JetBrainsMono.txt`). It is the **Nerd Fonts–patched** build (`JetBrainsMonoNerdFontMono-Regular`), required so box-drawing and `●○` glyphs render natively — plain JetBrains Mono lacks them and the fallback font breaks ASCII layouts. New ASCII/monospace UI must use it; never rely on system monospace fallback.
- **State:** `MainViewModel` (app list, selected app, live events), `Prefs` (SharedPreferences wrapper in `data/Prefs.kt`), constants in `data/Constants.kt`.
- **Settings:** `ui/SettingsFragment.kt` + `res/layout/fragment_settings.xml` are a hand-built terminal panel (ASCII section headers, mono labels, values on the right), not a PreferenceScreen. Live rows: set default, source, app info, pages, start page, ASCII splash, status bar, theme (long-press for system), text scale. No layout-land variant — the single scrolling layout is used in both orientations.
- **Splash:** `activity_main.xml` has a full-screen `splash` TextView. `MainActivity.onCreate` fills it with the `assets/ascii/logo.txt` figlet wordmark + `splash_tagline` and fades it out when `Prefs.asciiSplash` is on and this is a cold start.
- **Gestures:** `listener/OnSwipeTouchListener.kt` (attached to each pager page in `HomePagerAdapter`) and `listener/ViewSwipeTouchListener.kt` (empty-drawer overlay). On the pager home: horizontal swipe → page change (ViewPager2), swipe up → empty app drawer, swipe down → notification shade, long press → settings. The old left/right app-launch swipes and double-tap lock are no longer wired on home.
- **Drawer:** `AppDrawerFragment` + `AppDrawerAdapter` + `SearchView` + `RecyclerView`; overscroll at top closes it. With `DRAWER_EMPTY = true`, a full-screen `AsciiPanelView` titled `apps` replaces the list.
- **Build:** minSdk 24, compile/target SDK 36, JDK 17, AGP/Gradle 8.9.1, Kotlin 2.1.20. ViewBinding enabled.

## Rules

1. **Small, buildable commits.** Run `./gradlew assembleDebug` before committing. One concern per commit.
2. **Follow the existing stack.** XML + ViewBinding + ViewPager2. Don't mix paradigms.
3. **Preserve Olauncher behavior** unless the task explicitly replaces it. Don't silently delete gestures or prefs.
4. **ASCII art conventions:** static art (logos, wordmarks, empty-state pieces) lives in `app/src/main/assets/ascii/*.txt` and loads via `helper/AsciiAssets.kt` — never inline in Kotlin. Generated UI primitives (clock digit matrix in `AsciiClock`, progress bars) stay in code. Bundle the monospace font — never rely on system fallback. Render with fixed line height, `letterSpacing = 0`, and let `AsciiPanelView` shrink text to fit. Test on a narrow phone width.
5. **License:** GPL-3.0. Keep `LICENSE` and upstream attribution. Do not add code that can't ship under GPL-3.0.
6. **Package identity:** `app.ash.launcher` (debug: `app.ash.launcher.debug`). Keep it consistent in manifests, nav graphs, accessibility config, and release metadata.

## Roadmap state

- [x] Fork Olauncher base (full history, `upstream` remote kept for syncing)
- [x] Rebrand package / app identity (`app.ash.launcher`)
- [x] TUI home shell: boxed pages, tab bar, status/hint lines (2–5 pages configurable)
- [x] Empty app drawer (swipe up; no app list yet by design)
- [x] ASCII terminal settings panel
- [x] ASCII splash screen (toggleable; figlet wordmark, no mascot yet)
- [x] tty-clock-style block clock + figlet page art (`assets/ascii/`)
- [x] Local music page (MediaStore + MediaPlayer; play/pause, prev/next, ASCII progress)
- [ ] Command bar (app launch, calculator, quick note)
- [ ] Panels: notes, tasks, weather
- [ ] Theming pass (ash-and-ember palette + panel/tab/status chrome landed; dialog and icon leftovers remain)
- [ ] Keep `TESTING.md` in sync when features land

## Known gotchas

- **Gesture conflict resolved by pager:** horizontal swipes now page; the old swipe-left/right app-launch prefs remain in `Prefs` but are inert on the pager home. Don't silently delete them; decide the gesture mapping before rewiring.
- **Music permission:** `READ_MEDIA_AUDIO` on API 33+, `READ_EXTERNAL_STORAGE` (maxSdk 32) below. `HomeFragment` asks at most once per fragment (`permissionAsked`) when the music page becomes active; if permission is granted later from system settings, `onResume` re-checks. Never query MediaStore without checking `MusicPlayer.hasPermission`.
- **Music has no service:** keep `MusicPlayer` in `MainViewModel` (activity scope) so navigation doesn't kill playback. Don't move it into `HomeFragment` without adding a foreground service; don't claim background/lock-screen support.
- **Font is Nerd-patched.** Do not replace `res/font/jetbrains_mono_regular.ttf` with plain JetBrains Mono: box-drawing glyphs (`┌─┐│└┘`) and `●○` disappear and fallback metrics clip the ASCII frames (confirmed on device).
- **App drawer is intentionally empty.** Controlled by `DRAWER_EMPTY = true` in `AppDrawerFragment`; the layout keeps the search/recycler views hidden behind `emptyState`. Swipe down or tap closes it. Do not re-enable app listing without explicit instruction.
- **Home content replaced:** the original clock + 8-pinned-apps home layout is gone; `HomeFragment` was rewritten. Don't resurrect `homeApp1..8` or the old swipe listeners; add real content via pager pages instead. `feed` was replaced by `music` in `home_pages`; keep `TESTING.md` aligned with behavior changes.
- **fastlane/ metadata** was removed (it referenced upstream package ids). Add new publishing metadata only when a release pipeline exists.
- **Upstream promo system is gone.** `checkForMessages`, user-state review/share/pro/wallpaper dialogs, and their settings entry points were removed. Don't reintroduce review-begging or upstream links; keep `Constants.Dialog` unused for now.
- **Dead Olauncher settings were removed** (pinned-app count, date/time, alignment, auto-keyboard, daily wallpaper, bold font, swipe-apps rows, double-tap lock, hidden apps, share/rate/twitter). Their prefs and helper code remain in the data layer but have no UI; don't resurrect rows without an ash feature behind them.
- Old strings referencing "Olauncher" still exist in translated resources; clean up in the theming/polish pass.
