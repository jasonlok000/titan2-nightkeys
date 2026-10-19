"""Collect complete notices into source/APK; retire one-time migration tooling."""
from pathlib import Path
from urllib.request import urlopen
import hashlib
mit=Path('LICENSE').read_text()
r=mit.replace('Copyright (c) 2026 NightKeys contributors','Copyright (c) 2021 RikkaW')
f=mit.replace('Copyright (c) 2026 NightKeys contributors','Copyright (c) 2026 Furkan (furkan-bayrak)')
with urlopen('https://www.apache.org/licenses/LICENSE-2.0.txt',timeout=30) as response:
    apache=response.read().decode('utf-8')
assert 'Apache License' in apache and 'Version 2.0' in apache and len(apache)>9000
Path('LICENSES').mkdir(exist_ok=True)
Path('LICENSES/Apache-2.0.txt').write_text(apache)
notice='''# Third-party notices and research attribution

NightKeys application code, tests, documentation and original website artwork are MIT licensed.
No proprietary firmware, vendor APK, PhysiBoard implementation, or third-party font file is included.

## Resolved APK runtime dependencies

The 1.0.0-beta.1 releaseRuntimeClasspath resolves Shizuku api, provider, aidl and shared 13.1.5 (MIT, RikkaW) and AndroidX annotation 1.3.0 (Apache-2.0, Android Open Source Project). CI records the dependency graph; additions need a new notice review. Gradle/Android SDK and CI tools are build dependencies, not redistributed Android firmware.

## Shizuku API / provider / aidl / shared

'''+r+'''
## Hardware-interface research acknowledgement

`titan2-keylight` documents the Agui service interface. NightKeys independently implements that interface; its shell scripts are not redistributed. The project's MIT notice is retained below in acknowledgement.

'''+f+'''
PhysiBoard is GPLv3. Its device research informed earlier experiments, but no PhysiBoard implementation source is included. Neither project endorses NightKeys. See docs/SOURCES.md. Any future import of GPL code requires a separate licensing review.

## AndroidX annotations

Copyright The Android Open Source Project. Licensed under the Apache License, Version 2.0. Full text follows and is also in LICENSES/Apache-2.0.txt.

'''+apache
Path('THIRD_PARTY_NOTICES.md').write_text(notice)
Path('app/src/main/assets/THIRD_PARTY_NOTICES.txt').write_text(mit+'\n\n'+notice)
p=Path('app/src/main/java/com/everyonesniche/nightkeys/bridge/ShizukuShell.java')
s=p.read_text().replace('Shizuku API 13 still ships the implementation used by rish, but made the Java\n * entry point private while steering new apps toward UserService.', 'Shizuku API 13 still ships a legacy remote-process implementation. Its Java\n * entry point is private; upstream recommends UserService for new apps.');p.write_text(s)
p=Path('app/src/main/java/com/everyonesniche/nightkeys/core/BacklightSession.java')
s=p.read_text().replace('continuous while the display is on. The vendor itself hard-powers the LED off on\n * SCREEN_OFF. A durable snapshot is written before the first mutation and restored', 'continuous. NightKeys explicitly writes 0 plus CLOSE_KEYBOARD_LIGHT on screen-off;\n * the tested firmware can relight otherwise. A durable snapshot is saved and restored');p.write_text(s)
Path('.bootstrap/finalize.py').unlink()
Path('.github/workflows/finalize-repo.yml').unlink()
print('Complete APK notices collected; Apache text SHA256:',hashlib.sha256(apache.encode()).hexdigest())
