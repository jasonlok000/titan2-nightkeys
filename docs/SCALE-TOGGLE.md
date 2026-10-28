# Scale Toggle

Scale Toggle switches the Unihertz Titan 2 Elite between **phone mode** and **tablet mode**
by changing Android's display density through Shizuku. It has no screen of its own for the
switch: opening it *is* the switch. It's meant to be bound to a **long press of the red
side button**.

- User guide: <https://jasonlok000.github.io/titan2-nightkeys/scale-toggle.html>
- Releases: tags `scaletoggle-v<version>` in this repository (they don't trigger the
  NightKeys `v*` release workflow). Each release has the APK and a source zip.
- Package `dev.everyonesniche.scaletoggle`, signing certificate SHA-256
  `4060aaece81427fb1bf94469296f7a28aa9b8101b645dacf4a2a6f64b26e60b0` (unchanged since 1.0.2,
  so every later version installs over it).

## Default sizes

| Mode | Default | On the Titan 2 Elite (1076 px wide) |
|---|---|---|
| Phone mode | 300 DPI | about 574 dp: a phone layout with a little more on screen |
| Tablet mode | 286 DPI | about 602 dp, just past the 600 dp where many apps switch to two panes |

Before 1.2.0, phone mode aimed for 500 dp (`min(width, height) × 160 ÷ 500`, 344 DPI on
the Elite). A phone still in that mode is recognized as phone mode, so the next press goes
to tablet mode.

1.0.3 and 1.0.4 used 287 DPI (599.9 dp). Android rounds that to 600 dp, but Developer
options and apps that measure the width themselves see 599, so some apps stayed in their
phone layout. 1.0.5 moved to 286. A phone still at 287 is recognized as tablet mode, so the
next press goes to phone mode.

Only the display density changes. Font size, resolution and rotation are never touched.

## Settings (1.1.0 and later)

The settings are optional. Tapping the app or the side button always switches instantly.

**Opening settings:** long-press the Scale Toggle icon and tap **Settings** (a launcher app
shortcut), or open **Settings → Apps → Scale Toggle** and tap the gear (the
`APPLICATION_PREFERENCES` entry point).

**What you can set:** for each mode, **Default** or **Custom DPI**.

- The screen shows the current resolution and DPI, and each mode's default with the width it
  gives.
- While you type a custom DPI it previews the result, e.g. `601 dp wide · tablet layout`.
  The preview rounds down and calls 600 dp and above "tablet layout".
- **Save** stores the choice. Nothing changes on screen until the next switch.
- **Restore defaults** sets both modes back to Default and saves.

**Rules**

- A custom DPI is a whole number from **120 to 640**.
- Tablet mode's DPI must be **lower than** phone mode's. A mode left on Default counts as its
  default (phone 300, tablet 286), so a custom tablet DPI must be below 300 unless phone mode
  is custom too.
- Settings that break the rules can't be saved. If stored settings are ever unreadable or
  invalid, the switch ignores them and uses the defaults.

**Which mode comes next:** the switch reads the real current density first.

1. Equal to the tablet DPI (custom or default): switch to phone mode.
2. Equal to the phone DPI (custom or default): switch to tablet mode.
3. 287 (the 1.0.3/1.0.4 tablet value): switch to phone mode.
4. Otherwise: the 500/599/600 dp widths of earlier versions, then the last saved mode.

So changing settings mid-way never leaves the toggle stuck: at worst, one press lands in the
mode you were already in.

### Useful values

| Screen | Phone mode | Tablet mode |
|---|---|---|
| Titan 2 Elite, 1076 × 1200 | default (300); 344 for bigger text (500 dp) | default (286); try 280 if an app still shows its phone layout |
| Titan 2, 1440 × 1440 (untested) | **460** for 500 dp (the default 300 gives about 768 dp, already tablet-sized) | **384** for exactly 600 dp |

## Storage and privacy

- `scale_settings` preferences: `phone_dpi` and `tablet_dpi` (0 means Default).
- `scale_state` preferences: last mode, last applied density, the density before the first
  switch, and the last error.

Backup is disabled. There's no Internet permission, background service or notification.

## Source layout (in the release source zip)

| File | Role |
|---|---|
| `ScaleMath` | Presets, next-mode choice and DPI math. `Presets` holds the custom values. |
| `ScaleSettings` | Validation, number parsing and preview wording; free of Android UI so it's unit-tested. |
| `SettingsActivity` | The settings screen, built in code with platform views; no extra libraries. |
| `ScaleController` | Read, apply, verify and save, with rollback if verification fails. |
| `ToggleActivity` | The invisible launcher activity. Reads settings and runs the switch. |

`scripts/run-local-tests.sh` runs the JVM tests (all Android-free classes;
`SettingsActivity` is excluded because it is plain UI). They're local mock tests, not
on-device tests.
