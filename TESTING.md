# ash test checklist

Everything added since the Olauncher fork, in test order. Check items off as they pass.

## Build & install

- [ ] `export JAVA_HOME=~/.local/opt/jdk-17.0.21+9 && ./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`, tests green
- [ ] APK exists: `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Install on device (debug package `app.ash.launcher.debug`):
  - `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
  - MIUI note: `INSTALL_FAILED_USER_RESTRICTED` means "Install via USB" is still blocked — allow it in Developer options, or transfer with LocalSend and tap the APK.

## What was added

1. Rebrand to `ash` (package `app.ash.launcher`, label, icon, GPL-3.0 attribution kept)
2. ViewPager2 home with 2–5 configurable pages and configurable start page
3. TUI shell: tab bar, boxed page panels, status line, hint line
4. tty-clock-style block-digit clock with blinking colon and date
5. ASCII art: splash wordmark, figlet page art, music empty-state art
6. Terminal settings panel (live rows only)
7. Toggleable ASCII splash screen
8. Empty app drawer panel
9. Handset monospace font (Nerd-patched JetBrains Mono, box-drawing safe)
10. Local music player page (MediaStore + MediaPlayer)
11. Test docs and CI build
12. Visual polish: ash-and-ember palette, accent panel chrome, bracketed tabs, battery gauge, prompt line

## 1. Launch & brand

- [ ] Launcher icon and label read `ash` (not Olauncher)
- [ ] App opens to the home page on a cold start
- [ ] If ash is not the default launcher, `Set as default launcher` shows; tapping opens the chooser
- [ ] After setting default, the prompt disappears on next resume

## 2. Home shell, tabs, pages

- [ ] Tab bar shows `home notes tasks music more` (fewer entries if page count is lower)
- [ ] Active tab reads `[ home ]` in amber accent; inactive tabs are dim gray (no filled background)
- [ ] Panel border is dim, panel title is amber, page counter (`1/5`) sits in the bottom border in amber
- [ ] Tapping a tab switches pages; horizontal swipes also switch pages
- [ ] Status line (bottom) shows `HH:MM · <page>` and matches the active tab
- [ ] Battery gauge sits right of the status text in amber; bar fill and % match the system battery
- [ ] Hint line shows `← → pages · ↑ apps · hold settings` on non-music pages
- [ ] Page borders align on both edges; no truncated border glyphs
- [ ] On a narrow phone and at max text scale, panel content shrinks instead of clipping

## 3. Clock & ASCII art

- [ ] Home page shows the large block clock (tty-clock style, 2-char-thick strokes)
- [ ] Colon blinks once per second; layout does not shift when it blinks
- [ ] Date under the clock is lowercase `wed 2026-11-18` format
- [ ] Dim `~ $ _` prompt sits under the date; the cursor blinks in sync with the clock colon
- [ ] Clock stays correct after leaving and returning to the launcher
- [ ] `notes` and `tasks` pages show their figlet word art plus `nothing here yet`
- [ ] `more` page shows its figlet word art plus `nothing here yet`
- [ ] `music` page shows figlet `music` art while loading / without permission / with no tracks

## 4. Splash

- [ ] With ASCII splash ON: cold start flashes `ash` figlet wordmark + `minimal launcher`, then fades out (~1 s)
- [ ] Rotating/recreating the activity does not replay the splash
- [ ] With ASCII splash OFF: no splash on cold start
- [ ] Splash uses the mono font and does not clip at any text scale

## 5. Settings panel (long-press home)

- [ ] Panel renders box-drawing frames; section headers are ASCII
- [ ] `Set ash as default` row present and works
- [ ] `github` row opens https://github.com/charan-gn/ash-launcher
- [ ] `app info` opens the system app-info screen
- [ ] `pages` launches a 2/3/4/5 menu; choosing a value changes tab + pager count
- [ ] Lowering page count below the current start page clamps start page correctly
- [ ] `start page` menu lists 1..page-count and opens that page after leaving/returning to settings
- [ ] `ascii splash` toggles on/off and persists
- [ ] `status bar` toggles the system status bar immediately and persists
- [ ] `theme` cycles light → dark → system; long-press shows the system option
- [ ] `text size` dialog steps ±0.1 (0.5–1.5); OK recreates and persists
- [ ] Footer reads `ash 0.1.0`

## 6. App drawer

- [ ] Swipe up on the home pager opens the full-screen `apps` panel
- [ ] Panel shows the boxed `apps` title, `no apps yet`, and `swipe down to close`
- [ ] Swipe down or tap closes the drawer
- [ ] Drawer background matches the current theme

## 7. Gestures

- [ ] Swipe left/right changes pages (does not trigger old app-launch swipes)
- [ ] Swipe down opens the notification shade
- [ ] Long-press anywhere on the pager opens settings
- [ ] Back from settings/drawer returns home; back on home does nothing

## 8. Music page

- [ ] First time the music page is opened, Android asks for audio access
- [ ] Denying shows `grant audio access to play`; music art stays
- [ ] Granting loads the library and prepares the first track (title + artist visible)
- [ ] If files aren't tagged as music, longer audio (>10 s) is still found via the fallback query
- [ ] `no tracks found` shows when the device has no music
- [ ] Tap left third: previous track (or restarts the current one after 3 s)
- [ ] Tap center: play/pause toggles; control line swaps `[ play]` / `[pause]`
- [ ] Tap right third: next track, wrapping at the end
- [ ] Progress bar fills and `m:ss` current/total times advance once per second
- [ ] Track counter (`n / total`) matches the current track
- [ ] Unknown artist shows `unknown`; very long titles truncate cleanly at 28 chars
- [ ] Music keeps playing while swiping pages, opening the drawer/settings, or going to another app
- [ ] Unplugging headphones pauses playback (audio-becoming-noisy)
- [ ] Another app taking audio focus pauses playback
- [ ] Playback errors show `! playback error` instead of crashing
- [ ] Hint line on the music page changes to `tap edges skip · center play/pause`

## 9. Known limitations (expected, not bugs)

- No notification / lock-screen / Bluetooth media controls (no foreground service yet)
- No track list, search, seek bar, shuffle, repeat, or album art
- Music stops if Android kills the launcher process
- Notes/tasks/more pages are placeholders
- `feed` was replaced by `music` in the default page list
- Old unused Olauncher prefs/strings still exist in the data layer
