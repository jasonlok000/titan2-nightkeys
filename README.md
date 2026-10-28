# NightKeys · Titan Tools

**Your keyboard. Better after dark.**

NightKeys is a small, open-source Android utility for the **Unihertz Titan 2 Elite**. It keeps the physical keyboard lit in darkness, turns it off in a bright room, and explicitly turns it off when the screen sleeps. Your chosen stock keyboard brightness and keyboard app stay unchanged.

[Get the community beta](https://github.com/jasonlok000/titan2-nightkeys/releases/tag/v1.0.0-beta.3) · [Visual website & demo](https://jasonlok000.github.io/titan2-nightkeys/) · [Every-tap setup guide](https://jasonlok000.github.io/titan2-nightkeys/setup.html) · [Contribute](CONTRIBUTING.md)

## Before installing

This is **1.0.0-beta.3**, not a promise of compatibility with every Titan firmware. The working 0.9.7 lighting baseline was confirmed on a Titan 2 Elite running Android 16 / V02.00.04. This community build preserves that timing and vendor-control algorithm, with a new UI, normal source compilation and permanent release signing. The newly signed build still needs its physical check on each device.

Requirements: Titan 2 **Elite**, Android 16, main owner profile, ambient-light sensor, and a running, authorized [Shizuku](https://shizuku.rikka.app/). Wireless setup needs Wi-Fi; it does not need root or a computer. The regular Titan 2, other phones, managed profiles and root-started Shizuku have not been validated for this release.

**Always use Stop & restore before uninstalling, clearing data or stopping Shizuku.** The vendor setting is persistent. If NightKeys or Shizuku dies, a change can remain, including keyboard light while the display is off. This beta has no independent watchdog. See [Safety and recovery](docs/SAFETY.md).

## Install without guessing

Follow the [complete illustrated setup page](https://jasonlok000.github.io/titan2-nightkeys/setup.html), or the [plain-text guide](docs/SETUP.md). It covers every small step: installing Shizuku, exposing Developer options, pairing through its notification, starting the service, allowing APK installation from your browser, choosing keyboard brightness, and safe removal.

Download **NightKeys-1.0.0-beta.3.apk** from Releases, not a source ZIP. A GitHub account is not needed to download. **Beta.3 is signed with a new key** (the previous one was lost), so it cannot update beta.1, beta.2 or 0.9.x trials in place: tap Stop & restore in the old app, uninstall it, then install beta.3. Later releases keep the beta.3 certificate and update normally.

In NightKeys, the one-time setup sits above the Enable button: **1. Get Shizuku → 2. Start Shizuku → 3. Allow NightKeys**, then **Enable NightKeys**. One button always shows the next step, and finished steps get a tick. There is no timed test, brightness capture, or verification checklist. Android may ask for notification permission when enabling.

## What it changes

| Condition | Default behavior |
|---|---|
| Screen awake, fresh reading below 10 lux | Keyboard ON, immediately after the fresh reading |
| At or above 15 lux | Keyboard OFF after a 250 ms bright debounce |
| Between 10 and 15 lux | Keep the stable state while awake |
| Screen asleep | Timeout 0 plus the vendor close-light broadcast; sensing pauses |
| Stop & restore | Restore the exact timeout saved before the session |

The on threshold is adjustable. The off threshold is `max(on + 3, on × 1.5)`. Wake begins OFF and waits for fresh sensor data; it does not blindly reuse yesterday's darkness. The ON timeout is currently `2147483647` ms, not a renewable 60-second lease. Brightness is not changed; the maintainer's lowest visible stock value was 7.

No ads, analytics, account, keyboard-input capture, camera, microphone, location or Internet permission. Support reports are local and copied only when requested. [Privacy](PRIVACY.md).

## For developers

Java 17, Android SDK 36, Build-Tools 35.0.0. Android Gradle Plugin 8.13.2; checksum-verified Gradle 8.13 bootstrap.

```sh
git clone https://github.com/jasonlok000/titan2-nightkeys.git
cd titan2-nightkeys
bash tools/test-core.sh
bash gradlew :app:lintDebug :app:assembleDebug
```

The debug application ID ends in `.debug`; never run debug and release automation together. [Development](docs/DEVELOPMENT.md) · [Architecture](docs/ARCHITECTURE.md) · [Testing](docs/TESTING.md) · [Release procedure](docs/RELEASING.md).

## One home for future tools

The `site/` directory is **Titan Tools**: the public landing page, interactive light illustration, detailed guides, privacy information and contribution entry points. The catalogue is `site/data/tools.json`. Future tools can live in their own modules or repositories and appear here after review; they are not silently bundled with NightKeys. [Website development](docs/WEBSITE.md).

**Scale Toggle** (phone/tablet layout on a long press of the side button) is published here as `scaletoggle-v*` releases. [Guide](https://jasonlok000.github.io/titan2-nightkeys/scale-toggle.html) · [Defaults and settings](docs/SCALE-TOGGLE.md).

Help without coding: report a firmware result, improve a confusing instruction, translate a label, or suggest a narrowly scoped tool. [Start contributing](CONTRIBUTING.md).

## License and acknowledgements

NightKeys code, tests, documentation and original website artwork are MIT licensed. Shizuku API dependencies retain their MIT notices; AndroidX annotations use Apache-2.0. Device-interface research by [titan2-keylight](https://github.com/furkan-bayrak/titan2-keylight) and [PhysiBoard](https://github.com/brobata/physiboard) helped identify the vendor route. No proprietary firmware or PhysiBoard implementation is redistributed. [Full notices](THIRD_PARTY_NOTICES.md).

Independent community software maintained by [@jasonlok000](https://github.com/jasonlok000), not affiliated with or endorsed by Unihertz, Shizuku, or the research projects.
