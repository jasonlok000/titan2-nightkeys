# Research and dependency provenance

- Titan keyboard service interface and stock tile semantics: https://github.com/furkan-bayrak/titan2-keylight/blob/main/docs/TECHNICAL.md and its lib/common.sh. MIT project, copyright 2026 Furkan. NightKeys independently implements the documented agui_common reads/writes and close broadcast; it does not ship the scripts.
- Earlier Titan backlight experiments and controller notes: https://github.com/brobata/physiboard . PhysiBoard is GPLv3; its implementation is not included in NightKeys. Research about an interface is distinguished from copying its code. Do not import its source without a new license review.
- Shizuku API/source: https://github.com/RikkaApps/Shizuku-API . API, aidl, shared and provider 13.1.5 are MIT, copyright 2021 RikkaW. The separate Shizuku manager/server is installed by the user, not bundled into the release APK.
- Shizuku setup: https://shizuku.rikka.app/guide/setup/ and https://shizuku.rikka.app/download/ . CI uses the official Shizuku 13.6.0 release only in a disposable emulator.
- AndroidX annotations 1.3.0: https://android.googlesource.com/platform/frameworks/support/ ; Apache-2.0. The resolved releaseRuntimeClasspath is recorded in CI.
- Android platform/Gradle are build tools, not firmware redistributed by this repository. GitHub Actions retain their own licenses.

The final phone observations take precedence over general community claims: V02.00.04 could relight in sleep with a persistent huge timeout, so NightKeys explicitly forces OFF on screen-off. The owner confirmed the 0.9.7 baseline worked; that is not a claim that every future version or Elite firmware is tested.
