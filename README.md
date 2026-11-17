# ash

A minimal, terminal-inspired Android launcher with an ASCII soul.

`ash` starts from [Olauncher](https://github.com/tanujnotes/Olauncher) (GPL-3.0) and is being reshaped into a text-first home screen: monospace everywhere, box-drawing glyphs, a mascot, and horizontal pages that can eventually hold notes, tasks, weather, and quick actions — everything in one place, without the complexity of a traditional launcher.

Clean and quiet like Blloc Ratio, with personality from the terminal.

## Vision

- **Text-first UI.** App labels and ASCII glyphs instead of icon grids. One accent color, monospace type.
- **Linux feel, human-friendly.** Terminal aesthetics without making anyone memorize commands. Every gesture has a tap equivalent.
- **Pages, not clutter.** Swipe left/right between pages. Swipe up for the app drawer.
- **ASCII personality.** Splash logo, section headers in `┌─┐ │ └─┘` frames, status bars in `░▒▓█`, a small mascot with moods, typewriter reveals, blinking cursor.
- **Everything in one place.** Launcher, notes, tasks, weather, and a prompt-style command bar — built incrementally.

## Status

Early scaffold. The base launcher builds and runs; features are being layered on in small, reviewable commits.

- [x] Fork Olauncher base, full history, upstream remote kept for sync
- [x] Rebrand package / app identity (`app.ash.launcher`)
- [x] 3-page horizontal pager with text page indicator (`[●○○]`)
- [x] Empty app drawer (swipe up; intentionally no app list yet)
- [ ] ASCII splash screen + mascot
- [ ] Command bar (launch, calc, quick note)
- [ ] Panels: notes, tasks, weather
- [ ] Theming pass

## Tech stack

- Kotlin, XML views + ViewBinding (no Compose in this codebase)
- Navigation Component, single-activity, MVVM with `MainViewModel`
- `ViewPager2` for horizontal pages
- Min SDK 24, target/compile SDK 36, JDK 17

## Build

```sh
# requirements: JDK 17, Android SDK with platform 36 + build-tools
./gradlew assembleDebug
```

Debug builds install as `app.ash.launcher.debug`, so they can coexist with any other launcher.

## Contributing

Humans and AI agents both welcome. Read [`AGENTS.md`](AGENTS.md) before making changes — it documents the architecture, conventions, and current task state so agents don't guess.

Ground rules:

- Keep commits small and buildable; one concern per commit.
- Preserve Olauncher's existing behaviors unless a task explicitly replaces them.
- ASCII art lives in `assets/ascii/*.txt`, not inline strings.
- Monospace font must be bundled; test at narrow widths.

## License

GNU GPL-3.0. This is a fork of [Olauncher](https://github.com/tanujnotes/Olauncher) by Tanuj Notes; original license and attribution are preserved in [`LICENSE`](LICENSE). `ash` must be distributed under the same license.
