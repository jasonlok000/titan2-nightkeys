# Privacy and permissions

Updated 2026-10-19.

NightKeys reads ambient illuminance, screen state, basic device/firmware identity and an allow-listed set of vendor keyboard values. It changes `keyboard_brightness_timeout` and sends `agui.action.CLOSE_KEYBOARD_LIGHT`. It does not change stock brightness or the Quick Settings backup timeout.

There is no Internet, camera, microphone, location, contacts or overlay permission; no ads, analytics, login, keyboard-input capture or automatic upload. Guide/download links open the user's browser and are ordinary requests to those sites.

Private local preferences contain thresholds, setup verification, resume choice, last error and the original timeout recovery journal. Android app backup is disabled. The journal is cleared only after successful restoration; uninstalling or clearing data can destroy that restore point without resetting the vendor setting.

A support report is generated only on request and copied to the clipboard after an explanation. It contains model/firmware, light readings, state, timeout, recovery and error details. It does not intentionally include typed text or a device serial. The user must review it before posting to public GitHub issues.

Shizuku authorization allows shell-level operations and should only be granted to trusted apps. The quiet foreground-service notification exposes Stop & restore. A bounded partial wake lock is used during screen-off cleanup; no steady-state display wake lock is held.

The static Titan Tools website has no advertising, tracking scripts, third-party fonts or analytics. Its optional checklist uses localStorage on the user's browser; Reset removes it. The phone demo is an illustration, not sensor access. GitHub Pages and GitHub downloads process normal connection metadata under GitHub's privacy policy. Linked services have their own policies.

Before removing the app: start Shizuku, Stop & restore, confirm no pending hardware changes, then uninstall. See [setup](docs/SETUP.md) and [recovery](docs/SAFETY.md).
