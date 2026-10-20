# NightKeys setup — three steps

[Visual guide with every tap](https://jasonlok000.github.io/titan2-nightkeys/setup.html).
Guide checkmarks are optional browser-only notes, never app prerequisites.

## Get NightKeys

For Titan 2 Elite, Android 16, main owner profile. Baseline tested on V02.00.04;
other firmware is not automatically validated. Regular Titan 2 is not supported.
Download **NightKeys-1.0.0-beta.2.apk** from the [release](https://github.com/jasonlok000/titan2-nightkeys/releases/tag/v1.0.0-beta.2).
Open Downloads in your browser or Files, open the .apk and choose Install → Open.
Temporarily allow installs from that browser if Android asks; revoke that permission
afterward if desired. Do not disable Play Protect or ignore a harmful-app warning.

For community beta.1, Stop & restore, then install this update normally. It uses the
same permanent signing certificate. For older 0.9.x trials, Stop & restore, confirm
no pending recovery, uninstall once and install the community release.

## 1. Install Shizuku, if needed

NightKeys automatically shows Already installed when it finds Shizuku. Otherwise tap
Get Shizuku for [official downloads](https://shizuku.rikka.app/download/). Choose its
Play Store or official GitHub link, install, open and allow notifications. Return to
NightKeys. Shizuku saying Not running is normal before step 2.

## 2. Connect & authorize

Already running Shizuku? Tap Connect & authorize in NightKeys and allow it.
For first-time wireless setup, connect to trusted Wi-Fi. No computer or root is needed.

1. Phone Settings → About phone → tap Build number seven times; enter your own PIN if asked.
2. Back in Settings, search Developer options (often under System).
3. Follow Shizuku instructions to enable USB debugging and Wireless debugging. Do not enable OEM unlocking.
4. Open Shizuku → Pairing. Allow notifications if asked.
5. Open Wireless debugging details → Pair device with pairing code.
6. Keep the six-digit code window open. Pull down notifications → expand Shizuku pairing → Enter pairing code → send the code.
7. Return to Shizuku and separately tap Start. Wait for Running.
8. Return to NightKeys → Connect & authorize → Allow all the time.

Expected: Shizuku authorized / agui_common ready. This is a read-only service check,
not a timed lighting test. For pairing problems allow Shizuku notifications, check
Wi-Fi, make a fresh code and keep its dialog open. [Official help](https://shizuku.rikka.app/guide/setup/).

## 3. Enable NightKeys

Tap Enable NightKeys, and Allow if Android asks for notification permission. Done.
There is no timed test, saved firmware confirmation, or 90-second expiry. Leave the
app and use the phone. The running notification includes Stop & restore.

## Optional preferences, restart and removal

NightKeys keeps the existing stock keyboard brightness. Use Open keyboard brightness
settings to change it later; the maintainer's minimum reports 7, but there is no
calibration or raw number to enter. Default darkness threshold is 10 lux; lowering
it requires a darker room, raising it lights the keyboard sooner.

After reboot, start Shizuku and open NightKeys; normally pairing is remembered.
Allow unrestricted battery use for NightKeys/Shizuku if the ROM pauses them.
Avoid the stock keyboard-light tile while NightKeys is active.

Before uninstalling, clearing data or stopping Shizuku, Stop & restore and confirm
No pending hardware changes. If recovery fails, retain app data and reconnect
Shizuku to retry. The stock finite keyboard duration is a manual recovery option.
Unexpected process/helper death can leave the timeout changed; no independent
watchdog exists. [Safety details](SAFETY.md).
