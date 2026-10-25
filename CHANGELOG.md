# Changelog

## 1.0.0-beta.3 — 2026-10-25

- **New signing key.** The previous key was lost, so beta.3 cannot install over beta.1/beta.2: tap Stop & restore, uninstall the old app, then install beta.3. Future updates install normally again.

- Simpler main screen for first-time users: one top card shows the status, the live room light and the "How dark is dark enough?" slider together.
- One-time setup now sits above Enable: three steps (Get Shizuku, Start Shizuku, Allow NightKeys) with ticks and a single button for whichever step is next. It hides once setup is done and comes back if Shizuku stops.
- Technical status lines (Shizuku/agui_common checks) are replaced with plain-language hints.
- Removed the keyboard-brightness shortcut; brightness is still whatever you set in the phone's own settings.
- Help & reliability is folded into Advanced settings.
- Lighting policy, thresholds, recovery and signing are unchanged.

## 1.0.0-beta.2

- Replace the temporary check/confirmation flow with three steps: install Shizuku if absent, connect and authorize, enable.
- Detect installed Shizuku automatically, preserve real compatibility/recovery checks, and request notification permission during Enable.
- Remove the TEST action, 90-second timer and saved-verification prerequisites; lighting policy, stock brightness and recovery remain unchanged.
- Replace the generic landing-page handset with an original, proportion-matched Titan 2 Elite SVG and its physical key layout.
- Match the user guide to the same three steps; keep detailed pairing help and optional preferences.
- Add onboarding state coverage and UI/website regression checks.


## 1.0.0-beta.1 — 2026-10-19

First dedicated, open-source community beta.

- Preserve the phone-tested 0.9.7-rc8 lighting timing and `agui_common` shell route: immediate dark activation after a fresh sample, 250 ms bright debounce, explicit screen-off shutdown.
- Replace the laboratory UI with focused onboarding, an enable/restore action and a live light threshold.
- Remove manual hold/force-off controls and diagnostic monitor services from the installable app. Keep read-only, opt-in support export.
- Clearly separate the temporary 90-second check from everyday Enable NightKeys.
- Build normally from source; no patched-DEX distribution pipeline.
- Establish permanent community signing, release verification, CI and developer/contribution documentation.
- Introduce Titan Tools: responsive interactive showcase, complete setup guide and data-driven catalogue for future tools.

Compatibility: the earlier working baseline was confirmed on Titan 2 Elite V02.00.04 / Android 16. A newly signed community build still requires a physical check. No cross-device guarantee is implied.

Known limitations: persistent maximum ON timeout; no independent watchdog; Shizuku must be restarted after a phone reboot; stock keyboard-light tile can conflict. Read docs/SAFETY.md.
