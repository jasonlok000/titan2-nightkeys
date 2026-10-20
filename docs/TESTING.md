# Testing

## Automated gates

`bash tools/test-core.sh` runs deterministic policy/recovery tests, failure injection and randomized event sequences on the JVM. `tools/check-source.py` checks XML/resource references and source/manifest invariants. These do not exercise hardware.

CI builds debug, unsigned release and instrumentation APKs; runs Android lint; records resolved runtime dependencies; then starts an API 36 emulator and official Shizuku under shell UID 2000. The instrumentation tests authorize through the real dialog, verify actual privileged process identity and service command execution, reject failures/missing vendor services, check removed lab controls, and verify reconnect after force-stop. No fake Agui response is reported as a Titan success.

Website tests open the real static site on a local test server, exercise dark/bright/screen-off, verify local checklist persistence/reset, check internal links and desktop/mobile overflow, and capture screenshots. JavaScript-disabled guidance remains readable.

Artifacts record what actually ran. Test counts may change; use the current CI logs, not old trial claims.

## Physical release checklist

Use the candidate signed APK on a supported phone after restoring the previous version. Record firmware/app version. Keep stock brightness comfortable and do not use another backlight app or stock keyboard tile during a run.

- Dark room: enable, type nothing for over 45 seconds; selected brightness remains and keys stay lit.
- Lock: keys turn off and remain off after 0.5 s, 3 s and 30 s.
- Wake in darkness: keys relight; repeat at least ten cycles, including a short rapid off/on.
- Bright room: light goes off; move around the threshold without rapid flicker.
- Leave NightKeys for another app; repeat. Test a longer sleep period, not only short monitor cycles.
- Stop & restore: original timeout returns and the recovery journal clears.
- Restart Shizuku, reboot the phone and open NightKeys; verify permission/resume guidance and no false success.
- A firmware update requires repeating verification. Do not tick a failed physical-check box.

For purposeful force-stop/helper-failure tests, know the recovery procedure first. These can leave a persistent override. Never clear data to hide a failure. Consumer onboarding has no timed check. Enable NightKeys directly after Shizuku authorization; perform the hardware checks above as developer acceptance tests, not user prerequisites.
