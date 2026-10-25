# NightKeys setup — three steps

[Visual guide with every tap](https://jasonlok000.github.io/titan2-nightkeys/setup.html).
About 10 minutes the first time. Phone and Wi-Fi only: no computer, no root.

## Get NightKeys

For the Titan 2 Elite on Android 16, used by the phone's main user. Tested on software
version V02.00.04 (Settings → About phone). The regular Titan 2 is not supported.
Download **NightKeys-1.0.0-beta.3.apk** from the [release](https://github.com/jasonlok000/titan2-nightkeys/releases/tag/v1.0.0-beta.3).
Open it from your browser's download message or Files → Downloads, then Install → Open.
If Android blocks it, tap Settings → Allow from this source → back → Install. Do not
turn off Play Protect or ignore a harmful-app warning.

**Had beta.1 or beta.2?** Beta.3 uses a new signing key, so it cannot install over
them. Open the old NightKeys, tap Stop & restore, uninstall it, then install beta.3.
Shizuku stays paired. Later updates install normally.

NightKeys opens with a **One-time setup** card above the **Enable NightKeys** button.
One big button always shows the next step, and finished steps get a tick.

## 1. Get Shizuku

Shizuku is a free helper app that lets NightKeys control the keyboard light without
root. If it is already installed, NightKeys ticks this step automatically. Otherwise
tap **Get Shizuku**, install it from Google Play or its official GitHub link, open it
once and allow notifications. Shizuku saying "Not running" is normal at this point.

## 2. Start Shizuku

Tap **Open Shizuku**. If Shizuku has never been started, set it up once:

1. Connect to Wi-Fi. Settings → About phone → tap Build number 7 times (enter your own PIN if asked).
2. Settings → Developer options: turn on USB debugging and Wireless debugging. Never enable OEM unlocking.
3. In Shizuku, under Start via Wireless debugging, tap Pairing.
4. Settings → Developer options → tap the words Wireless debugging → Pair device with pairing code. Keep the code open.
5. Pull down Shizuku's notification → Enter pairing code → type the 6 digits.
6. Back in Shizuku, tap **Start** and wait for Running.

Pairing is once only. Starting is needed again after every phone restart.
[Official Shizuku help](https://shizuku.rikka.app/guide/setup/).

## 3. Allow NightKeys

Back in NightKeys, tap **Allow NightKeys** and choose **Allow all the time** in the
Shizuku pop-up. NightKeys shows "Ready for the dark" and "✓ Setup done". If it says it
couldn't reach the keyboard light, tap **Try again**.

## Turn it on

Tap **Enable NightKeys** and allow notifications if asked. Close the app; it keeps
working. In a dark room the keys stay lit while the screen is on. In a bright room,
or with the screen off, the keyboard light turns off.

## Adjusting, restarting and removing

- **How dark is dark enough?** The slider in NightKeys' top card. Left = only in very
  dark rooms; right = lights up sooner. Default: on below 10 lux, off at 15 lux.
- **Brightness:** set it in the phone's own keyboard backlight setting. NightKeys keeps it.
- **After a restart:** NightKeys shows the setup steps again. Open Shizuku → Start,
  then tap Enable NightKeys if it's paused. No need to pair again.
- **Keeps stopping?** Advanced settings → Battery settings → Unrestricted (same for Shizuku).
- **Before uninstalling, clearing data or stopping Shizuku:** tap **Stop & restore**.
  If NightKeys or Shizuku is killed unexpectedly, the keyboard timeout can stay
  changed; there is no independent watchdog. Start Shizuku and tap Stop & restore to
  fix it, or choose a normal time in the stock keyboard backlight settings.
  [Safety details](SAFETY.md).
- Avoid the stock keyboard-light quick tile while NightKeys is on.
