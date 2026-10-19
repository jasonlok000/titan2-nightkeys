"""One-time removal of the trial lab; lighting policy and transport remain intact."""
from pathlib import Path
import re, shutil
root = Path('.')
for name in ('DiagnosticSession', 'LeaseGuard'):
    old = root / f'app/src/main/java/com/everyonesniche/nightkeys/core/{name}.java'
    new = root / f'core-tests/fixtures/{name}.java'
    new.parent.mkdir(parents=True, exist_ok=True)
    if old.exists(): shutil.move(old, new)
for name in ('app/src/main/java/com/everyonesniche/nightkeys/DiagnosticLog.java',
             'app/src/main/java/com/everyonesniche/nightkeys/service/DiagnosticMonitorService.java',
             'tools/build_patched_apk.py', 'SHA256SUMS.txt', 'build-apk.sh', 'build-apk.cmd'):
    Path(name).unlink(missing_ok=True)
p = Path('app/src/main/java/com/everyonesniche/nightkeys/NightKeysApp.java')
s = p.read_text().replace('    public DiagnosticLog diagnostics;\n', '').replace('journal = new RecoveryJournal(this); diagnostics = new DiagnosticLog(this);', 'journal = new RecoveryJournal(this);').replace('        diagnostics.noteAppProcessStart();\n','')
p.write_text(s)
p = Path('app/src/main/java/com/everyonesniche/nightkeys/Config.java')
s = p.read_text(); start = s.index('    public String diagnosticMode()'); end = s.index('    public static boolean supportedIdentity()', start)
p.write_text((s[:start]+s[end:]).replace('Capture a minimum for this firmware first','Confirm your keyboard brightness first'))
p = Path('app/src/main/java/com/everyonesniche/nightkeys/service/BootReceiver.java')
s=p.read_text(); start=s.index('        // A diagnostic hardware test'); end=s.index('        if (!app.journal.pending()',start)
p.write_text(s[:start]+s[end:])
p=Path('app/src/main/AndroidManifest.xml')
s=p.read_text(); s=re.sub(r'        <service android:name=".service.DiagnosticMonitorService".*?</service>\n','',s,flags=re.S); p.write_text(s)
p=Path('app/src/main/java/com/everyonesniche/nightkeys/bridge/BridgeClient.java')
s=p.read_text().replace('Shizuku stopped. Start it again; the phone still turns the keyboard off with the screen.', 'Shizuku stopped. Restart it, then open NightKeys. A saved keyboard change may still need restoration.'); p.write_text(s)
p=Path('app/src/main/java/com/everyonesniche/nightkeys/service/LightingService.java')
s=p.read_text(); assert 'app.config.offThreshold(),0,250' in s
p.write_text(s.replace('Minimum brightness requested · no typing timeout','Your chosen keyboard brightness · continuous lighting').replace('Lighting paused safely','Lighting paused'))
p=Path('tools/test-core.sh')
p.write_text(p.read_text().replace('"$ROOT/core-tests/CoreTests.java"','"$ROOT"/core-tests/fixtures/*.java "$ROOT/core-tests/CoreTests.java"'))
p=Path('tools/test-core.cmd')
s=p.read_text().replace('"%ROOT%core-tests\\CoreTests.java"','"%ROOT%core-tests\\fixtures\\*.java" "%ROOT%core-tests\\CoreTests.java"');p.write_text(s)
p=Path('app/src/androidTest/java/com/everyonesniche/nightkeys/bridge/PermissionTests.java')
s=p.read_text().replace('import com.everyonesniche.nightkeys.DiagnosticLog;\n','').replace('import com.everyonesniche.nightkeys.service.DiagnosticMonitorService;\n','')
start=s.index('        test("diagnostic lifecycle');end=s.index('\n    }',start)
s=s[:start]+'''        test("production screen has no diagnostic hardware controls", () -> {
            check(context.getResources().getIdentifier("diag_hold", "id", context.getPackageName()) == 0,
                    "Diagnostic hold control leaked into the app");
            check(context.getResources().getIdentifier("diag_force_off", "id", context.getPackageName()) == 0,
                    "Diagnostic off control leaked into the app");
        });'''+s[end:]
p.write_text(s)
# Keep licensing visible in the app even before the expanded dependency audit lands.
a=Path('app/src/main/assets');a.mkdir(exist_ok=True)
(a/'THIRD_PARTY_NOTICES.txt').write_text(Path('LICENSE').read_text()+'\n\n'+Path('THIRD_PARTY_NOTICES.md').read_text())
Path('.bootstrap/productionize.py').unlink()
Path('.github/workflows/prepare-community.yml').unlink()
print('Trial lab removed. Automatic policy retains the tested 0 / 250 ms timing.')
