# Architecture and invariants

## Scope

NightKeys is a standalone utility, not an input method. It never reads typed keys. The community beta keeps the working 0.9.7 algorithm, not the earlier speculative lease proposal.

Normal path:

```text
Foreground service → SensorManager + screen state → LightPolicy
        → BacklightSession → AndroidPort → authorized Shizuku process
        → /system/bin/service → agui_common → Unihertz controller
```

Shizuku starts shell UID 2000 without root in the tested path. The app uses the legacy remote-process API still present in pinned Shizuku API 13.1.5. That private/legacy entry point is a compatibility risk; do not claim future Shizuku versions are supported without integration tests. UserService was not reliably connected on the initial phone experiments. There is no generic command UI.

## Vendor contract

Read: `service call agui_common 3 s16 keyboard_brightness_timeout s16 <missing sentinel>`.
Write: `service call agui_common 4 s16 keyboard_brightness_timeout s16 <validated integer>`.
Close: `am broadcast -a agui.action.CLOSE_KEYBOARD_LIGHT`.

Only the main timeout is written. The bridge may read timeout, backup, brightness and auto flag for support. Stock brightness and backup are untouched. Parcel parsing rejects malformed/error output; a process exit of zero alone is not enough. Writes are verified by rereading the key. These observations are not physical luminance measurement.

## Policy

Default on threshold 10 lux; off threshold `max(T + 3, 1.5T)`, normally 15. Dark transition delay 0; bright transition 250 ms. While awake, the gap retains a stable classification. Screen-off invalidates the awake sample, commands OFF and unregisters the sensor. Wake starts OFF and waits for a fresh sample, rather than assuming an old reading is still true.

ON uses `2147483647` ms. OFF uses `0` plus CLOSE. Heartbeat/readback is **not** a renewable finite lease and does not imply a one-minute fail-safe. Rewriting a persistent timeout is not proof of resetting a firmware timer. Changing this is a separate hardware experiment.

The maintainer observed a roughly 0.5 s keyboard relight when a huge timeout was left active while asleep. Therefore retain the explicit screen-off write and broadcast; do not defer to stock screen-off behavior alone.

## Concurrency and recovery

The service serializes normal policy/sensor/hardware work on a worker looper. A separate emergency executor handles screen-off even when normal work is delayed, with a bounded 4 s partial wake lock. Screen gating is also checked around hardware operations. Recovery is durable before the first mutation. Failed restoration keeps the journal and exposes a retry instead of claiming success.

A service restart recovers an outstanding original before claiming a new session. Intentional Stop restores it. Simultaneous process/helper failure is not independently guarded. The UI must never clear a journal to make a button appear ready.

## Release boundaries

Production UI is separate from historical test fixtures; no manual hold/off actions are shipped. Support reports are opt-in, read-only and local. APK startup on unsupported identity must not alter hardware. All hardware writes must stay behind explicit authorization, owner-profile/device checks and the existing session/recovery ownership model.

Changes to these invariants require regression tests, command-trace review and a real-device report. An emulator has neither `agui_common` nor the Titan LEDs.

## Onboarding (beta.2)

Three steps: detect/install Shizuku, start/connect/authorize, enable. `Onboarding.canStart` checks supported identity, sensor presence, channel readiness and absence of an active session/pending recovery. It does not read a trial/calibration/verification flag. Legacy saved flags are ignored, never converted into evidence of hardware validation. There is no TEST service action or expiry timer. Android notification permission is requested as part of Enable. Core light thresholds, timing, screen gates, vendor transport and journal behavior are unchanged.
