# Changelog

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
