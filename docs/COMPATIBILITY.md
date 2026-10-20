# Compatibility

| Device / software | Evidence | Status |
|---|---|---|
| Titan 2 Elite, Android 16, V02.00.04, main owner | Maintainer confirmed working 0.9.7-rc8 baseline after isolated hold/off/sensor/wake tests | Baseline physically verified; community reports welcome, no onboarding test gate |
| Android 16 emulator + official non-root Shizuku | CI tests shell UID, permission, service command, missing-vendor refusal and reconnect | Transport/test environment only; not Titan hardware |
| Other Elite firmware | No reviewed report yet | Experimental; physical verification required |
| Regular Titan 2, Titan Pocket, other models | Not supported by release identity gate | Unsupported |
| Work profile / secondary Android user | Not supported by identity gate | Unsupported |
| Root-started Shizuku | Not independently validated | Not advertised as tested |

Model text can say `Unihertz Titan 2` on an Elite; the build display/fingerprint also identify Elite. Minimum APK API is 36 (Android 16). Shizuku API 13 is the tested backend family. The maintainer's minimum visible brightness is 7; this is not asserted to be a universal minimum.

To add evidence, open the Firmware compatibility issue form. Report exact app version and build display, all four physical lighting checks, several wake cycles, reboot/reconnect, and safe restoration. Separate observed behavior from guesses. Do not mark a device supported just because `service list` or a settings readback succeeds.
