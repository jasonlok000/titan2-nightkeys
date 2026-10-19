# Build and develop NightKeys

## Requirements

Git, Java 17, Android SDK Platform 36 and Build-Tools 35.0.0. Install Android Studio, open SDK Manager, install those packages, and select JDK 17 for Gradle. CLI users should set `ANDROID_HOME` to their SDK directory or put `sdk.dir=<absolute SDK path>` in untracked `local.properties`.

The project uses Android Gradle Plugin 8.13.2 and Gradle 8.13. `gradlew` is a small, readable Java source bootstrap (`tools/GradleBootstrap.java`), not a patched APK tool. It downloads the pinned Gradle distribution and verifies SHA-256. First build requires Internet access for Gradle, Android and Maven dependencies. Do not replace download/checksum failures with unverified binaries.

```sh
git clone https://github.com/jasonlok000/titan2-nightkeys.git
cd titan2-nightkeys
bash tools/test-core.sh
python3 tools/check-source.py
bash gradlew --no-daemon :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
```

On Windows use `gradlew.bat` and `tools\test-core.cmd`; Python is `py -3` when installed through the Windows launcher. Android Studio can open the repository directly. SDK path must point to an installed SDK, not this repository.

## Output and installation

- Debug: `app/build/outputs/apk/debug/app-debug.apk`, package `com.everyonesniche.nightkeys.debug`.
- Unsigned release without signing environment: `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Release with documented signing environment: `app-release.apk`, package `com.everyonesniche.nightkeys`.

Debug and release may coexist, but **never enable both at once**: they edit the same vendor setting. Restore before uninstalling either. `adb install -r <apk>` installs a compatible same-key update; a key mismatch is not something to bypass.

The debug APK is for development, not community distribution. Never commit a keystore or use a default debug certificate for public releases.

## Code map

`core/LightPolicy`: deterministic ambient/screen policy. `core/BacklightSession`: original snapshot, writes/readback, restore. `hardware/AndroidPort`: screen checks and narrow bridge adapter. `bridge/BridgeClient` and `ShizukuShell`: authorized shell-UID transport. `service/LightingService`: foreground lifecycle, sensor registration and screen-off emergency lane. `ui/MainActivity`: onboarding and read-only support report. `Config` and `RecoveryJournal`: durable app state.

Historical DiagnosticSession/LeaseGuard exist only in `core-tests/fixtures`; no lab controls or monitor are packaged. The interface file is not permission to add arbitrary commands.

Tests and tooling: [TESTING](TESTING.md). Privileged implementation constraints: [ARCHITECTURE](ARCHITECTURE.md). Website edits: [WEBSITE](WEBSITE.md). Maintainer publishing: [RELEASING](RELEASING.md).
