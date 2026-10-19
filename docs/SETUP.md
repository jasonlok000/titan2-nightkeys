# NightKeys setup — every step

The [visual guide](https://jasonlok000.github.io/titan2-nightkeys/setup.html) has a checklist and expandable help. Its complete source is in `site/setup.html`; the app and guide are maintained together.

## 1. Check the phone

Open Settings (gear icon) → About phone. Check Android version and Build number. This beta is for **Titan 2 Elite / Android 16 / main owner profile**. The model can say Titan 2 even on an Elite; the build display identifies Elite. V02.00.04 is the tested working baseline. Regular Titan 2 and other models are not supported. Connect to trusted Wi-Fi.

## 2. Install Shizuku

Use [the official download page](https://shizuku.rikka.app/download/), then its Play Store or official GitHub link. Tap Install, then Open. Allow Shizuku notifications. “Not running” is normal before setup; installing is not starting.

If using an APK, open the completed download. Android may open a permission screen for your browser: temporarily enable Allow from this source, return and install. Disable that source permission afterwards if desired. Do not disable Play Protect or ignore a harmful-app warning.

## 3. Enable debugging

In Settings → About phone, tap Build number **seven times**. Enter your own phone PIN when Android asks; do not share it. Return to Settings and search Developer options (often under System). Follow Shizuku's prompt to enable USB debugging and Wireless debugging on your trusted Wi-Fi. Tap the Wireless debugging **row**, not just its switch, to open details. Do not enable OEM unlocking or unlock the bootloader.

## 4. Pair and start

Open Shizuku → Pairing. In Wireless debugging, tap Pair device with pairing code. Keep the six-digit code window open. Pull down notifications, expand Shizuku's pairing notification and tap Enter pairing code. Type the code and send it. Return to Shizuku and separately tap **Start** under wireless debugging. Wait until it says running.

No notification? Long-press Shizuku's app icon → App info → Notifications and allow them. Check Wi-Fi and Wireless debugging, generate a fresh pairing code, and keep its window open. Further help: [official Shizuku setup](https://shizuku.rikka.app/guide/setup/).

## 5. Install NightKeys

Open [the community beta release](https://github.com/jasonlok000/titan2-nightkeys/releases/tag/v1.0.0-beta.1). Under Assets select **NightKeys-1.0.0-beta.1.apk**, not Source code ZIP. No GitHub login is needed for a public download. Open the file from the browser or Files → Downloads. If Android asks, allow installation from that browser, return to the installer and tap Install → Open. You can revoke that install-source permission afterwards.

Already using a 0.9.x trial? First open it, Stop & restore, and confirm its report has no pending hardware changes. Then uninstall the trial. This first community release uses a different permanent certificate; future community updates must use that same new certificate.

## 6. Connect and choose brightness

In NightKeys tap **Connect Shizuku**. In the Shizuku prompt approve NightKeys with Allow all the time. Wait for `Shizuku authorized` and `agui_common ready`. Tap Open keyboard brightness settings. If it opens Display settings, find or search for Keyboard backlight. Set a comfortable low keyboard brightness. NightKeys leaves it unchanged; the maintainer's minimum was 7, but you do not enter a raw number. No automatic-brightness toggle is required. Return to NightKeys and keep the threshold at 10 lux initially.

## 7. Check, save, then enable

In a dark room choose Check my keyboard · 90 seconds, read the message, begin, and allow notifications. Wait **45 seconds without typing**: the keys should stay lit. Lock the screen and watch several seconds: the keys must turn off and stay off. Wake in darkness: they should relight. Move into bright light: they should switch off. Do not use the stock keyboard-light tile during this test.

Choose Save successful check and tick only what you actually observed. If anything failed, do not confirm it; stop and report. When restoration finishes, tap **Enable NightKeys**. The temporary check alone does not enable everyday automation.

## 8. Reliability and removal

Open NightKeys battery & app settings → Battery/App battery usage → Unrestricted if offered. Menu wording varies. Apply the same to Shizuku if it keeps stopping.

After phone reboot: connect to Wi-Fi, re-enable Wireless debugging if necessary, open Shizuku → Start, then open NightKeys. Pairing is usually remembered. If it does not resume, tap Enable NightKeys.

To pause, use Stop & restore, not Force stop. Before uninstalling, clearing data or stopping Shizuku: start Shizuku, Stop & restore, confirm **No pending hardware changes**, then remove it. If recovery fails, keep the app data and select a normal finite duration in stock Keyboard backlight settings as a manual recovery measure. It is not guaranteed to be your previous value. Copy a reviewed support report and open a bug.

The persistent ON timeout can remain after process/helper death; on tested firmware it can cause light while the display sleeps. There is no independent watchdog. The stock keyboard tile can conflict with NightKeys; pause NightKeys before using it. See [Safety](SAFETY.md).
