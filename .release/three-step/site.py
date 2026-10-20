from pathlib import Path
import re,json,html
ROOT=Path.cwd();site=ROOT/'site'
# Original SVG; references the manufacturer's dimensions and key placement.
phone=['''<div class="phone" data-phone data-lit="true" data-screen="true" aria-hidden="true">
<svg class="elite-phone" viewBox="0 0 316 486" xmlns="http://www.w3.org/2000/svg">
<defs>
  <linearGradient id="elite-frame" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#74767b"/><stop offset=".03" stop-color="#383a3d"/><stop offset=".5" stop-color="#595b5f"/><stop offset=".97" stop-color="#2a2b2d"/><stop offset="1" stop-color="#85868b"/></linearGradient>
  <linearGradient id="elite-cap" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#3a3c3f"/><stop offset=".5" stop-color="#292b2e"/><stop offset="1" stop-color="#191b1e"/></linearGradient>
  <clipPath id="elite-display"><rect x="20" y="18" width="276" height="306.667" rx="24"/></clipPath>
</defs>
<!-- 75 mm wide / 117.8 mm tall body, 1080:1200 display, top-left punch hole. -->
<rect x="307" y="86" width="3" height="87" rx="1.5" fill="#777b7e"/>
<rect x="307" y="209" width="4" height="51" rx="2" fill="#666a70"/>
<rect x="307" y="296" width="3.5" height="34" rx="1.5" fill="#c0473e"/>
<rect x="6" y="126" width="3" height="80" rx="1.5" fill="#63666a"/>
<rect x="8" y="6" width="300" height="471.2" rx="34" fill="url(#elite-frame)" stroke="#8c8d90" stroke-width=".8"/>
<rect x="11" y="9" width="294" height="465.2" rx="31" fill="#292b2d" stroke="#0b0c0d" stroke-width="1.5"/>
<path d="M108 9h99" stroke="#080a0b" stroke-width="2" stroke-linecap="round"/>
<path d="M52 7v3m218-3v3M52 473v4m218-4v4" stroke="#949697" stroke-width="3"/>
<rect x="16" y="14" width="284" height="314.667" rx="28" fill="#07090a"/>
<rect class="elite-screen-bg" x="20" y="18" width="276" height="306.667" rx="24" fill="#192729"/>
<g class="elite-screen-content" clip-path="url(#elite-display)">
 <path d="M-25 300Q100 125 344 223M-30 326Q98 155 339 250" fill="none" stroke="#a9c99a" stroke-opacity=".10" stroke-width="1"/>
 <text x="59" y="37" fill="#c8d1cd" font-size="8" font-family="sans-serif">22:14</text>
 <path d="M257 28v5m3-7v7m3-9v9" stroke="#c8d1cd" stroke-width="1.5"/>
 <rect x="273" y="25" width="14" height="7" rx="2" fill="none" stroke="#c8d1cd" stroke-width=".7"/><rect x="275" y="27" width="10" height="3" rx=".5" fill="#c8d1cd"/>
 <path d="M167 104a24 24 0 1 0 19 34 24 24 0 0 1-19-34Z" stroke="#d9f3b2" fill="none" stroke-width="1.4"/>
 <text class="screen-title" x="158" y="195" text-anchor="middle">NightKeys</text>
 <text class="screen-caption" x="158" y="217" text-anchor="middle">A little light. Just enough.</text>
 <rect x="135" y="306" width="46" height="2" rx="1" fill="#c4cfca" opacity=".6"/>
</g>
<circle cx="40" cy="38" r="9.4" fill="#080a0d"/><circle cx="40" cy="38" r="4.8" fill="#0d1422"/><circle cx="41" cy="37" r="1.3" fill="#455576" opacity=".7"/>
<rect x="15" y="333" width="286" height="128" rx="10" fill="#131516" stroke="#424447" stroke-width="1"/>
<g class="elite-keyboard">
''']
rows=[list(zip('QWERTYUIOP',['0','1','2','3','(',')','_','−','+','@'])),list(zip(['A','S','D','F','G','H','J','K','L','backspace'],['*','4','5','6','/',':','#',"'",'"',''])),list(zip(['alt','Z','X','C','V','B','N','M','sym','enter'],['','7','8','9','?','!',',','.','','']))]
icons={'backspace':'<path d="M-5-3h8v6h-8l-3-3Z M-2-1.5l3 3m0-3-3 3"/>','enter':'<path d="M5-5v5H-5m3-3-3 3 3 3"/>','shift':'<path d="m0-6 5 5H2v5h-4v-5h-3ZM-3 6h6"/>','back':'<path d="m3-5-7 5 7 5Z"/>','home':'<circle cx="0" cy="0" r="4"/>','space':'<path d="M-10 4h20"/>','overview':'<rect x="-3.7" y="-3.7" width="7.4" height="7.4"/>'}
def key(x,y,w,label,alt=''):
    cap=f'<g class="elite-key" data-key="{html.escape(label)}"><path d="M{x:.2f} {y:.2f}h{w:.2f}v27h-{w:.2f}Z" fill="url(#elite-cap)" stroke="#090b0c" stroke-width=".8"/><path d="M{x+1:.2f} {y+1:.2f}h{w-2:.2f}m0 1v24" fill="none" stroke="#73787c" stroke-opacity=".25" stroke-width=".65"/>'
    if label in icons: cap+=f'<g class="key-icon" transform="translate({x+w/2:.2f} {y+16:.2f})">{icons[label]}</g>'
    else:
        size=8 if label in ('alt','sym','fn') else 12
        cap+=f'<text class="key-legend" x="{x+w/2:.2f}" y="{y+22:.2f}" text-anchor="middle" font-size="{size}">{html.escape(label)}</text>'
    if alt: cap+=f'<text class="key-alt" x="{x+4:.2f}" y="{y+8:.2f}" font-size="6.5">{html.escape(alt)}</text>'
    return cap+'</g>\n'
for r,row in enumerate(rows):
    phone.append(f'<g class="elite-key-row" data-row="{r+1}">')
    for i,(label,alt) in enumerate(row): phone.append(key(22+i*27.2,336+r*31,27.2,label,alt))
    phone.append('</g>')
x=22;phone.append('<g class="elite-key-row" data-row="4">')
for label,w in [('shift',31),('back',31),('home',31),('space',86),('overview',31),('fn',31),('shift',31)]:
    phone.append(key(x,429,w,label));x+=w
phone.append('</g></g><circle cx="241" cy="465" r="2" fill="#0d0f11"/></svg></div>')
svg=''.join(phone)
f=site/'index.html';s=f.read_text();start=s.index('<div class="phone"');end=s.index('</div></div><div class="demo-caption"',start)
s=s[:start]+svg+s[end+6:]
s=s.replace('Requires Shizuku. Our guide explains every step.', 'Install Shizuku → connect → enable. No timed test.')
s=s.replace('An illustration, not a live connection to your phone.', 'Titan 2 Elite illustration · not a live phone connection.')
s=s.replace('Other firmware needs testing first','Other firmware compatibility is not yet confirmed');f.write_text(s)
css='''/* Titan 2 Elite front-view schematic. Original vector geometry, reference in docs/WEBSITE.md. */
.phone{width:292px;max-width:80%;padding:0;border:0;background:none;box-shadow:none;position:relative;transform:rotate(-5deg);filter:drop-shadow(0 26px 26px #0005)}
.phone:before{content:none}.elite-phone{display:block;width:100%;height:auto;overflow:visible}
.elite-phone text{font-family:ui-sans-serif,system-ui,sans-serif;font-weight:400;letter-spacing:0}
.elite-phone .screen-title{font-size:25px;fill:var(--paper);letter-spacing:-.9px}
.elite-phone .screen-caption{font-size:10px;fill:var(--muted)}
.elite-screen-bg,.elite-screen-content{transition:fill .25s,opacity .25s}
.elite-phone .key-legend,.elite-phone .key-alt{fill:#95999b;transition:fill .25s,filter .25s}
.key-icon{stroke:#95999b;fill:none;stroke-width:1;stroke-linecap:round;stroke-linejoin:round;transition:stroke .25s,filter .25s}
.phone[data-lit=true] .key-legend,.phone[data-lit=true] .key-alt{fill:#f7f8f4;filter:drop-shadow(0 0 1.8px #f5fff185)}
.phone[data-lit=true] .key-icon{stroke:#f7f8f4;filter:drop-shadow(0 0 1.8px #f5fff185)}
.phone[data-screen=false] .elite-screen-bg{fill:#030506}
.phone[data-screen=false] .elite-screen-content{opacity:0}
'''
f=site/'assets/styles.css';s=f.read_text();start=s.index('.phone{');end=s.index('.demo-caption{',start);s=s[:start]+css+s[end:]
s=s.replace('.phone{width:280px}.phone-screen{height:292px}.key{height:27px;max-width:25px}', '.phone{width:308px}')
s=s.replace('.phone{width:230px;transform:rotate(-5deg)}.phone-screen{height:235px}.key{height:22px;font-size:9px}', '.phone{width:276px;transform:rotate(-4deg)}')
s+='\n.setup-download{margin-bottom:30px;padding:24px;border:1px solid var(--line);border-radius:14px}.setup-download h2{font-size:25px;letter-spacing:-.035em;margin-bottom:12px}.setup-download p{margin-block:12px;font-size:15px;color:var(--muted)}.setup-download .button{margin-block:8px}.setup-after{padding-top:30px;border-top:1px solid var(--line)}.setup-after h2{font-size:27px;letter-spacing:-.035em;margin-bottom:20px}.setup-after p{margin-block:16px}.mini-steps{display:flex;gap:18px;margin-top:22px;flex-wrap:wrap;color:var(--green);font-size:14px}\n';f.write_text(s)
old=(site/'setup.html').read_text();header=old[:old.index('<main')];footer=old[old.index('<footer'):]
content='''<main id="main"><div class="page-hero wrap"><p class="breadcrumb"><a href="./">Titan Tools</a> / Setup</p><h1>Three steps.<br>Then let there be light.</h1><p class="lede">Install Shizuku if you need it. Connect and authorize. Enable NightKeys. No timed test, brightness capture, or checklist inside the app.</p><div class="mini-steps"><span>01 · Install</span><span>02 · Connect</span><span>03 · Enable</span></div></div>
<div class="guide-layout wrap"><aside class="guide-nav" aria-label="Setup navigation"><h2>Your setup</h2><ol><li><a href="#shizuku">Install Shizuku</a></li><li><a href="#connect">Connect &amp; authorize</a></li><li><a href="#enable">Enable NightKeys</a></li></ol><p><a href="#download">Get the NightKeys app</a></p><p><a href="#reliable">Restart, brightness &amp; removal</a></p><p class="progress-text js-only" id="setup-progress" aria-live="polite">0 of 3 steps complete</p><button id="reset-progress" type="button" class="reset js-only">Reset guide progress</button><p class="fine">Optional guide checkmarks stay in this browser. They never control the app.</p></aside>
<div class="steps">
<div class="setup-download" id="download"><h2>First, get NightKeys</h2><p>For <strong>Titan 2 Elite, Android 16, main owner profile</strong>. The working baseline is firmware V02.00.04. A regular Titan 2 is not the same phone.</p><a class="button" href="https://github.com/jasonlok000/titan2-nightkeys/releases/download/v1.0.0-beta.2/NightKeys-1.0.0-beta.2.apk">Download NightKeys APK</a><p>Open the completed download, then tap <strong>Install → Open</strong>. If Android asks, temporarily allow installation from that browser. You can turn that permission off afterwards. No GitHub account is needed.</p><details><summary>Where do I find the file? What about Android warnings?</summary><p>Look in your browser’s Downloads menu or the Files app → Downloads. The filename ends in <strong>.apk</strong>, not .zip or .idsig. On the release page, it is under Assets. Do not disable Play Protect or ignore a harmful-app warning.</p></details><details><summary>Already have an earlier NightKeys?</summary><p>Tap <strong>Stop &amp; restore</strong> in the old app first. Community beta.1 updates normally to this version with the same signing key; no uninstall needed. Older 0.9.x trials need one uninstall because they used a different key. Do not remove an old app with a pending recovery record.</p></details><p>Open NightKeys. Its three setup steps match the guide below.</p></div>
<section class="step" id="shizuku"><div class="step-heading"><span class="step-number">01</span><h2>Install Shizuku, if needed</h2></div><div class="step-body"><p>Shizuku lets NightKeys change the keyboard timeout without rooting your phone. <strong>NightKeys detects it automatically</strong>: if step 1 says “Already installed,” go straight to step 2.</p><ol><li>Tap <strong>Get Shizuku</strong> in NightKeys. It opens the official download page.</li><li>Choose its Play Store or official GitHub download. Install Shizuku and open it. Allow its notifications so the pairing prompt can appear.</li><li>Return to NightKeys. Step 1 now shows that Shizuku is installed.</li></ol><p><a href="https://shizuku.rikka.app/download/">Official Shizuku downloads ↗</a></p><p class="expect">“Not running” in Shizuku is normal at this point. Installing it is not the same as starting it.</p><label class="done"><input type="checkbox" data-step-check="shizuku">Shizuku is installed</label></div></section>
<section class="step" id="connect"><div class="step-heading"><span class="step-number">02</span><h2>Connect &amp; authorize</h2></div><div class="step-body"><p>If Shizuku already says <strong>Running</strong>, skip the first-time setup below. In NightKeys, tap <strong>Connect &amp; authorize</strong>, then approve NightKeys in the Shizuku prompt.</p><details open><summary>First time using Shizuku? Follow these taps.</summary><ol><li>Connect your phone to a trusted Wi-Fi network. No computer is needed for this method.</li><li>Open the phone’s <strong>Settings → About phone</strong>. Tap <strong>Build number seven times</strong>. Enter your own lock-screen PIN if Android asks; never share it.</li><li>Return to Settings and search for <strong>Developer options</strong> (usually under System). Follow Shizuku’s instructions to enable <strong>USB debugging</strong> and <strong>Wireless debugging</strong>. Do not enable OEM unlocking.</li><li>Open <strong>Shizuku → Pairing</strong>. Allow notifications if asked.</li><li>Go to <strong>Wireless debugging</strong>. Tap the row to open its details, not just its switch. Tap <strong>Pair device with pairing code</strong>.</li><li>Keep the six-digit code window open. Pull down the notification shade and expand Shizuku’s pairing notification. Tap <strong>Enter pairing code</strong>, enter those six digits and send them.</li><li>Go back to Shizuku. Separately tap <strong>Start</strong> under wireless debugging. Wait for it to say <strong>Running</strong>.</li><li>Return to NightKeys. Tap <strong>Connect &amp; authorize</strong>. Choose <strong>Allow all the time</strong> in the Shizuku prompt.</li></ol></details><details><summary>No pairing notification, or Shizuku will not start?</summary><p>Allow Shizuku notifications in Settings → Apps → Shizuku → Notifications. Check Wi-Fi and Wireless debugging, generate a new pairing code and keep its window open. Pairing alone is not enough: return to Shizuku and tap Start. <a href="https://shizuku.rikka.app/guide/setup/">Official setup help ↗</a></p></details><p class="expect">NightKeys should report “Shizuku authorized” and “agui_common ready.” This is a read-only compatibility check, not a timed lighting test.</p><label class="done"><input type="checkbox" data-step-check="connect">NightKeys is connected and authorized</label></div></section>
<section class="step" id="enable"><div class="step-heading"><span class="step-number">03</span><h2>Enable NightKeys</h2></div><div class="step-body"><ol><li>Tap <strong>Enable NightKeys</strong>. You do not need to run a test or save a confirmation.</li><li>If Android asks for notification permission, tap <strong>Allow</strong>. This provides a quiet running notification with a Stop &amp; restore control.</li><li>That’s it. Leave NightKeys and use your phone. In darkness, the keyboard stays lit while your screen is on. In a bright room or with the screen off, the keyboard switches off.</li></ol><p>It uses your existing keyboard brightness and a default threshold of 10 lux. You can change those later under <strong>Optional preferences</strong>.</p><p class="expect">No 90-second expiry. Automatic lighting stays enabled until you stop it, or the service cannot continue.</p><label class="done"><input type="checkbox" data-step-check="enable">NightKeys is enabled</label></div></section>
<div class="setup-after" id="reliable"><h2>A few useful things to know</h2><details><summary>Choose the minimum brightness</summary><p>In NightKeys, open Optional preferences → Open keyboard brightness settings. Use the stock slider to choose the lowest level you can comfortably see. NightKeys does not alter it. The maintainer’s Elite reports value 7 at minimum, but you never need to type a number or confirm a calibration.</p></details><details><summary>Make it work in a darker or brighter room</summary><p>Under Optional preferences, lower the threshold to require a darker room, or raise it to light the keys sooner. The default turns on below 10 lux and off at 15 lux; the gap avoids flicker.</p></details><details><summary>After restarting your phone</summary><p>Start Shizuku again, then open NightKeys. You normally do not need to pair again. Resume can be enabled in NightKeys, but Android may block automatic restart. Tap Enable NightKeys if it is paused.</p></details><details><summary>Keep it running in the background</summary><p>Open NightKeys battery &amp; app settings → Battery or App battery usage → Unrestricted, if available. Do the same for Shizuku if it keeps stopping. Labels vary by firmware. NightKeys keeps a quiet running notification.</p></details><details><summary>Pause or uninstall safely</summary><p>Use <strong>Stop &amp; restore</strong> before uninstalling, clearing data or stopping Shizuku. Confirm the support report says no pending hardware changes. If restoration fails, keep the app data and start Shizuku to retry; a normal finite duration in stock Keyboard backlight settings is a manual recovery option.</p></details><p class="warning">This is a community beta. If the app or Shizuku is killed unexpectedly, the persistent timeout can remain changed and the keyboard may stay lit while asleep. There is no independent watchdog. If the light behaves incorrectly, use Stop &amp; restore and <a href="https://github.com/jasonlok000/titan2-nightkeys/issues/new?template=bug_report.yml">report it</a>.</p><p class="fine">Avoid the stock keyboard-light Quick Settings tile while NightKeys is enabled; both edit the same timeout. Pause NightKeys before using that tile.</p></div>
</div></div></main>'''
(site/'setup.html').write_text(header+content+footer)
f=site/'assets/site.js';s=f.read_text().replace("'titan-tools-setup-v1'", "'titan-tools-setup-v2'");f.write_text(s)
f=site/'data/release.json';v=json.loads(f.read_text());v={k:x.replace('beta.1','beta.2') if isinstance(x,str) else x for k,x in v.items()};f.write_text(json.dumps(v,indent=2)+'\n')
f=ROOT/'README.md';s=f.read_text().replace('1.0.0-beta.1','1.0.0-beta.2')
s=re.sub(r'In NightKeys:.*?everyday automation enabled\.', 'In NightKeys: **1. Install Shizuku if needed → 2. Connect & authorize → 3. Enable NightKeys.** Already-installed Shizuku is detected automatically. There is no timed test, brightness capture, or verification checklist. Android may ask for notification permission when enabling.',s);f.write_text(s)
(ROOT/'docs/SETUP.md').write_text('''# NightKeys setup — three steps

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
''')
f=ROOT/'docs/TESTING.md';s=f.read_text().replace('The 90-second onboarding check is separate from the Enable NightKeys action.', 'Consumer onboarding has no timed check. Enable NightKeys directly after Shizuku authorization; perform the hardware checks above as developer acceptance tests, not user prerequisites.');f.write_text(s)
f=ROOT/'docs/COMPATIBILITY.md';s=f.read_text().replace('Baseline physically verified; community beta must repeat check','Baseline physically verified; community reports welcome, no onboarding test gate');f.write_text(s)
f=ROOT/'docs/ARCHITECTURE.md';s=f.read_text()+'\n## Onboarding (beta.2)\n\nThree steps: detect/install Shizuku, start/connect/authorize, enable. `Onboarding.canStart` checks supported identity, sensor presence, channel readiness and absence of an active session/pending recovery. It does not read a trial/calibration/verification flag. Legacy saved flags are ignored, never converted into evidence of hardware validation. There is no TEST service action or expiry timer. Android notification permission is requested as part of Enable. Core light thresholds, timing, screen gates, vendor transport and journal behavior are unchanged.\n';f.write_text(s)
f=ROOT/'docs/WEBSITE.md';s=f.read_text()+'''\n## Titan 2 Elite illustration\n\nThe interactive handset is an original inline SVG, not a manufacturer photograph.
It follows the Elite's **117.8 × 75 mm** body proportions and **1080 × 1200** display,
with a top-left punch-hole, three full ten-key rows plus the shift/navigation/space/fn
row, and the red side shortcut button. The previous generic floating keycaps and
invented printed branding have been removed. The UI inside the screen is illustrative.
Reference: [Unihertz product page](https://www.unihertz.com/products/titan-2-elite),
front view `T2US-E1-US-01.jpg`. No manufacturer artwork is redistributed.
The `.key-legend`, `.key-alt`, `.key-icon` and `.elite-screen-content` classes expose
lighting states to the existing demo. Test dark, bright, sleep and the light slider.
Setup has exactly three `.step` sections; download and optional help are not steps.
''';f.write_text(s)
f=ROOT/'tools/package-release.sh';s=f.read_text().replace('This newly signed community build still needs its physical setup check.', 'Consumer setup is install, authorize, enable. No timed-check gate; expanded real-device validation is still encouraged.');f.write_text(s)
f=ROOT/'CHANGELOG.md';s=f.read_text();intro='''\n## 1.0.0-beta.2\n\n- Replace the temporary check/confirmation flow with three steps: install Shizuku if absent, connect and authorize, enable.\n- Detect installed Shizuku automatically, preserve real compatibility/recovery checks, and request notification permission during Enable.\n- Remove the TEST action, 90-second timer and saved-verification prerequisites; lighting policy, stock brightness and recovery remain unchanged.\n- Replace the generic landing-page handset with an original, proportion-matched Titan 2 Elite SVG and its physical key layout.\n- Match the user guide to the same three steps; keep detailed pairing help and optional preferences.\n- Add onboarding state coverage and UI/website regression checks.\n\n''';p=s.find('\n##');s=s[:p]+intro+s[p:];f.write_text(s)
(ROOT/'docs/releases/1.0.0-beta.2.md').write_text('''# NightKeys 1.0.0-beta.2\n\n## Three-step setup\n\n1. Install Shizuku if it is not already installed (automatically detected).\n2. Start Shizuku, return to NightKeys, and connect/authorize.\n3. Enable NightKeys. Android may ask for notification permission.\n\nNo 90-second check, saved hardware-confirmation checklist, or brightness capture.\nThe working lighting algorithm remains unchanged: default 10/15 lux, immediate dark\nactivation, 250 ms bright debounce, explicit OFF on screen-off and exact original\ntimeout restoration. Your stock brightness remains untouched. The installer still\nrefuses unsupported hardware, unavailable transport, and unresolved recovery.\n\nThe Titan Tools page now has a proportion-matched Titan 2 Elite illustration and\nphysical keyboard layout, retaining the interactive dark/bright/sleep demonstration.\n\n## Install/update\n\nFor community beta.1: Stop & restore, then install the APK as a normal update; the\npermanent certificate is unchanged. For 0.9.x trials: restore first and uninstall\nonce because those trial signing keys differ. There is no need to repeat setup\nverification because that prerequisite no longer exists.\n\n## Limitations\n\nTitan 2 Elite, Android 16, owner profile; baseline owner-tested on V02.00.04.\nUnexpected app/Shizuku termination can leave the persistent timeout changed. No\nindependent watchdog. Always Stop & restore before removal, clearing data or stopping\nShizuku. APK readback and emulator tests cannot measure the physical LEDs.\n''')
for f in site.glob('*.html'): f.write_text(f.read_text().replace('1.0.0-beta.1','1.0.0-beta.2'))
f=ROOT/'tools/test-site.py';s=f.read_text().replace('count()==8','count()==3').replace('data-step-check="phone"','data-step-check="shizuku"').replace('1 of 8 steps','1 of 3 steps')
s=s.replace("            assert page.locator('h1').is_visible()",'''            assert page.locator('h1').is_visible()
            assert page.locator('.elite-key-row').count()==4
            assert page.locator('[data-row="1"] .elite-key').count()==10
            assert page.locator('[data-row="2"] .elite-key').count()==10
            assert page.locator('[data-row="3"] .elite-key').count()==10
            assert page.locator('[data-row="4"] .elite-key').count()==7''',1)
s=s.replace("            page.screenshot(path=str(output/f'home-{label}.png'),full_page=True)","            page.screenshot(path=str(output/f'home-{label}.png'),full_page=True)\n            page.screenshot(path=str(output/f'hero-{label}.png'),full_page=False)")
s=s.replace("assert page.locator('.step').count()==3\n", "assert page.locator('.step').count()==3\n            assert page.locator('.step h2').all_text_contents()==['Install Shizuku, if needed','Connect & authorize','Enable NightKeys']\n")
s=s.replace('browser=p.chromium.launch(headless=True)', "browser=p.chromium.launch(headless=True, executable_path=os.environ.get('PLAYWRIGHT_CHROMIUM_EXECUTABLE'))");f.write_text(s)
print('Patched accurate Elite demo, three-step guide and current documentation.')
