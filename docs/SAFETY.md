# Safety, limitations and recovery

NightKeys changes a persistent vendor keyboard setting. It is not a safety-critical controller and this beta has no independent watchdog. A successful settings readback does not prove the LEDs are physically off.

## Normal shutdown

With Shizuku running, use **Stop & restore** in the app or notification. Open **Copy support report** and confirm `Recovery: No pending hardware changes.` before uninstalling, clearing data or stopping Shizuku. The restored timeout is the exact saved value, not an assumed 3 or 30 seconds.

## Unexpected process/helper death

A huge ON timeout can remain stored if both cleanup and recovery are unavailable. On tested V02.00.04 firmware, leaving it active can relight the keyboard even with the screen off. Do not describe the current design as automatically expiring in one minute. It does not use the proposed renewable finite lease.

Restart Shizuku, open NightKeys and restore. Keep the app and its data until restoration succeeds. If this route fails, select an ordinary finite duration in stock Keyboard backlight settings as a manual recovery measure, then report the failure. That manual choice is not a guaranteed restoration of the earlier preference.

The stock keyboard-light Quick Settings tile uses a backup timeout and may overwrite NightKeys' live value. Pause NightKeys before using it. Do not run another backlight controller or a debug and release NightKeys simultaneously.

## Reboot and firmware updates

Non-root Shizuku needs restarting after phone reboot. Open NightKeys afterwards; Android may block background foreground-service starts. Firmware changes invalidate the previous physical verification. Repeat all lighting checks before enabling on another build.

No power-saving claim or measured battery-cost guarantee is made. No unsupported device should be forced past identity/service checks. Do not probe random vendor transaction numbers, write hardware nodes, or disable Android security protections to get through setup.
