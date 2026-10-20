from pathlib import Path
import re
ROOT = Path.cwd()
main = ROOT/'app/src/main'
java = main/'java/com/everyonesniche/nightkeys'
f = java/'Config.java'
s=f.read_text(); start=s.index('    /**'); end=s.index('    public boolean wanted()', start)
s=s[:start]+s[end:]
s=re.sub(r'    public void testStarted\(\).*?\n', '', s)
s=re.sub(r'    public boolean hasTest\(\).*?\n', '', s); f.write_text(s)
(java/'RuntimeState.java').write_text('''package com.everyonesniche.nightkeys;

/** Immutable UI snapshot: requested/read-back state, never measured LED output. */
public final class RuntimeState {
    public final boolean running, lightRequested;
    public final float lux;
    public final String title, detail;
    public RuntimeState(boolean running, boolean lightRequested, float lux, String title, String detail) {
        this.running = running; this.lightRequested = lightRequested;
        this.lux = lux; this.title = title; this.detail = detail;
    }
    public static RuntimeState idle() {
        return new RuntimeState(false, false, Float.NaN, "Ready when you are",
                "Install Shizuku, connect it, then enable NightKeys.");
    }
}
''')
(java/'core/Onboarding.java').write_text('''package com.everyonesniche.nightkeys.core;

/** No calibration, firmware-verification flag, trial or elapsed-time gate. */
public final class Onboarding {
    private Onboarding() { }
    public static boolean canStart(boolean supported, boolean sensor, boolean connected,
                                   boolean running, boolean pendingRecovery) {
        return supported && sensor && connected && !running && !pendingRecovery;
    }
}
''')
f=java/'service/LightingService.java';s=f.read_text()
s=s.replace('    public static final String TEST = "com.everyonesniche.nightkeys.TEST";\n','')
s=s.replace('registered, testing, stopping','registered, stopping').replace('private long testEnd, lastPublish, lastRenew;', 'private long lastPublish, lastRenew;')
s=re.sub(r'    private final Runnable testExpiry = \(\) -> \{.*?\n    \};\n','',s,flags=re.S)
start=s.index('                if (START.equals(action)) {'); end=s.index(' else if (REFRESH.equals(action)',start)
s=s[:start]+'''                if (START.equals(action)) {
                    restoreOnly = false; app.config.wanted(true);
                }'''+s[end:]
s=s.replace('if (!testing && !app.config.wanted())','if (!app.config.wanted())').replace('(!testing && !app.config.wanted())','!app.config.wanted()')
s=re.sub(r'            if \(!app.config.captured\(\).*?\n                throw new IllegalStateException\([^\n]+\);\n','',s)
s=s.replace('            if (testing) app.config.testStarted();\n','')
s='\n'.join(line for line in s.split('\n') if not ('if (testing &&' in line))
s=s.replace('(),testing,', '(),').replace('session.isLit(),testing,','session.isLit(),')
s=s.replace('title,detail,testEnd)', 'title,detail)')
s=s.replace('String notification = testing ? "Compatibility test · " + detail : detail;', 'String notification = detail;')
s=s.replace('testing = false; restoreOnly = true;', 'restoreOnly = true;')
s=s.replace(' worker.removeCallbacks(testExpiry);','')
s=s.replace('new RuntimeState(false,false,false,Float.NaN,','new RuntimeState(false,false,Float.NaN,')
s=s.replace('completeMessage,0)', 'completeMessage)').replace('clean(error),0)', 'clean(error))').replace('"Reopen NightKeys to resume or restore.",0)', '"Reopen NightKeys to resume or restore.")')
assert 'testing' not in s and 'testExpiry' not in s and 'verified' not in s
assert 'app.config.offThreshold(),0,250' in s; f.write_text(s)
f=java/'ui/MainActivity.java';s=f.read_text()
s=s.replace('import android.os.SystemClock;\n','').replace('import android.widget.LinearLayout;\n','')
s=s.replace('import com.everyonesniche.nightkeys.RuntimeState;', 'import com.everyonesniche.nightkeys.RuntimeState;\nimport com.everyonesniche.nightkeys.core.Onboarding;\nimport rikka.shizuku.Shizuku;')
s=s.replace('dragging, showSetup;', 'dragging;').replace('accessLabel, error;', 'accessLabel, error, installLabel, enableDetail;')
s=s.replace('private Button primary, test, confirm;', 'private Button primary, install, authorize;')
s=s.replace('        test = findViewById(R.id.test);\n        confirm = findViewById(R.id.confirm_test);', '''        install = findViewById(R.id.install_shizuku);
        authorize = findViewById(R.id.authorize);
        installLabel = findViewById(R.id.install_label);
        enableDetail = findViewById(R.id.enable_detail);''')
start=s.index('        findViewById(R.id.authorize).setOnClickListener'); end=s.index('        findViewById(R.id.guide)', start)
s=s[:start]+'''        install.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/"))));
        authorize.setOnClickListener(v -> {
            if (!shizukuInstalled() || !shizukuRunning()) openShizuku();
            else app.bridge.authorize();
        });
        findViewById(R.id.open_shizuku).setOnClickListener(v -> openShizuku());
        findViewById(R.id.open_keyboard).setOnClickListener(v -> openKeyboardSettings());
'''+s[end:]
s=s.replace('        boolean supported = Config.supportedIdentity() && light != null;', '''        boolean supported = Config.supportedIdentity() && light != null;
        boolean installed = shizukuInstalled();
        boolean server = shizukuRunning();''')
s=s.replace('!active && app.config.verified()', '!active && app.bridge.ready()')
start=s.index('            if (state.testing) {'); end=s.index('\n        error.setText',start)
s=s[:start]+'''            statusDetail.setText(state.detail);
        }
        primary.setText(active || pending ? R.string.stop_restore : R.string.enable);
        primary.setEnabled(active || pending || Onboarding.canStart(Config.supportedIdentity(),
                light != null, app.bridge.ready(), active, pending));
        primary.setAlpha(primary.isEnabled() ? 1f : .5f);
        installLabel.setText(installed ? R.string.shizuku_installed : R.string.install_detail);
        install.setVisibility(installed ? View.GONE : View.VISIBLE);
        accessLabel.setText(!installed ? getString(R.string.install_first) : !server
                ? getString(R.string.start_shizuku_detail) : app.bridge.status());
        authorize.setText(app.bridge.ready() ? R.string.reconnect
                : server ? R.string.authorize : R.string.start_shizuku);
        authorize.setEnabled(installed && !active && !pending);
        findViewById(R.id.open_shizuku).setVisibility(installed ? View.VISIBLE : View.GONE);
        enableDetail.setText(active ? R.string.enabled_detail : pending ? R.string.restore_detail
                : app.bridge.ready() ? R.string.enable_ready : R.string.enable_waiting);
'''+s[end:]
start=s.index('            new AlertDialog.Builder(this).setTitle(R.string.notification_title)'); end=s.index('\n        } else command(action);',start)
s=s[:start]+'''            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS);'''+s[end:]
start=s.index('    private void confirmPhysicalCheck() {'); end=s.index('    private void openKeyboardSettings()',start)
s=s[:start]+'''    private boolean shizukuInstalled() {
        try {
            getPackageManager().getApplicationInfo("moe.shizuku.privileged.api",
                    PackageManager.ApplicationInfoFlags.of(0));
            return true;
        } catch (PackageManager.NameNotFoundException e) { return false; }
    }
    private boolean shizukuRunning() {
        try { return Shizuku.pingBinder(); }
        catch (RuntimeException e) { return false; }
    }
    private void openShizuku() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        open(launch == null ? new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/")) : launch);
    }
'''+s[end:]
s=s.replace(' + "\\nSetup checked: " + app.config.verified() + "\\nAutomation requested: " + app.config.wanted()', ' + "\\nAutomation requested: " + app.config.wanted()')
assert not any(x in s for x in ('verified()', '.testing', 'confirm_test', 'R.id.test', 'showSetup', 'LightingService.TEST')); f.write_text(s)
(main/'res/layout/activity_main.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root" android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:background="@color/background">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:padding="24dp">
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/app_name"
            android:textSize="18sp" android:fontFamily="sans-serif-medium" android:textColor="@color/accent"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="20dp"
            android:text="@string/headline" android:textSize="30sp" android:fontFamily="sans-serif-medium" android:textColor="@color/text"/>
        <TextView style="@style/Body" android:text="@string/subtitle"/>
        <LinearLayout style="@style/Card">
            <TextView android:id="@+id/status_title" style="@style/SectionTitle" android:text="@string/welcome_title" android:accessibilityLiveRegion="polite"/>
            <TextView android:id="@+id/status_detail" style="@style/Body" android:text="@string/welcome_detail"/>
            <TextView android:id="@+id/lux_value" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:layout_marginTop="12dp" android:text="@string/unknown_lux" android:textColor="@color/accent" android:textSize="26sp"/>
            <com.everyonesniche.nightkeys.ui.LuxMeterView android:id="@+id/lux_meter"
                android:layout_width="match_parent" android:layout_height="54dp" android:importantForAccessibility="no"/>
            <TextView android:id="@+id/error" style="@style/Body" android:textColor="@color/warning" android:visibility="gone" android:accessibilityLiveRegion="polite"/>
        </LinearLayout>
        <LinearLayout style="@style/Card">
            <TextView style="@style/SectionTitle" android:text="@string/step_install"/>
            <TextView android:id="@+id/install_label" style="@style/Body" android:text="@string/install_detail"/>
            <Button android:id="@+id/install_shizuku" style="@style/SecondaryButton" android:text="@string/install_shizuku"/>
        </LinearLayout>
        <LinearLayout style="@style/Card">
            <TextView style="@style/SectionTitle" android:text="@string/step_connect"/>
            <TextView android:id="@+id/access_label" style="@style/Body" android:text="@string/access_initial" android:accessibilityLiveRegion="polite"/>
            <Button android:id="@+id/authorize" style="@style/PrimaryButton" android:text="@string/authorize"/>
            <Button android:id="@+id/open_shizuku" style="@style/SecondaryButton" android:text="@string/open_shizuku"/>
            <Button android:id="@+id/guide" style="@style/SecondaryButton" android:text="@string/guide"/>
        </LinearLayout>
        <LinearLayout style="@style/Card">
            <TextView style="@style/SectionTitle" android:text="@string/step_enable"/>
            <TextView android:id="@+id/enable_detail" style="@style/Body" android:text="@string/enable_waiting"/>
            <Button android:id="@+id/main_action" style="@style/PrimaryButton" android:text="@string/enable"/>
            <TextView style="@style/Body" android:text="@string/enable_note"/>
        </LinearLayout>
        <LinearLayout style="@style/Card">
            <TextView style="@style/SectionTitle" android:text="@string/preferences_title"/>
            <TextView android:id="@+id/threshold_label" style="@style/Body" android:text="@string/threshold_initial" android:labelFor="@id/threshold"/>
            <SeekBar android:id="@+id/threshold" android:layout_width="match_parent" android:layout_height="56dp"
                android:contentDescription="@string/threshold_title" android:progressTint="@color/accent" android:thumbTint="@color/accent"/>
            <TextView style="@style/Body" android:text="@string/threshold_detail"/>
            <TextView style="@style/Body" android:text="@string/brightness_detail"/>
            <Button android:id="@+id/open_keyboard" style="@style/SecondaryButton" android:text="@string/open_keyboard"/>
        </LinearLayout>
        <LinearLayout style="@style/Card">
            <TextView style="@style/SectionTitle" android:text="@string/help_title"/>
            <CheckBox android:id="@+id/resume_boot" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:minHeight="56dp" android:text="@string/resume_boot" android:textColor="@color/text" android:textSize="14sp"/>
            <TextView style="@style/Body" android:text="@string/restart_detail"/>
            <Button android:id="@+id/battery" style="@style/SecondaryButton" android:text="@string/battery"/>
            <Button android:id="@+id/support" style="@style/SecondaryButton" android:text="@string/copy_report"/>
            <TextView style="@style/Body" android:text="@string/safety_note" android:textColor="@color/warning"/>
        </LinearLayout>
        <TextView android:id="@+id/version" style="@style/Body" android:layout_marginTop="24dp" android:gravity="center"/>
        <Button android:id="@+id/licenses" style="@style/SecondaryButton" android:text="@string/licenses"/>
        <TextView style="@style/Body" android:text="@string/independent" android:gravity="center" android:layout_marginBottom="12dp"/>
    </LinearLayout>
</ScrollView>
''')
f=main/'res/values/strings.xml';s=f.read_text()
remove=['begin_check','test_temporary','check_title','check_instructions','check_countdown','save_check_title','save_check_detail','check_brightness','check_stays','check_screen','check_bright','save_check','now_enable','setup_again','setup_title','setup_detail','notification_title','notification_detail','continue_label']
for name in remove: s=re.sub(r'    <string name="'+name+'">.*?</string>\n','',s)
s=s.replace('Connect Shizuku and check the keyboard once. Then enable NightKeys.', 'Install Shizuku if needed, connect it, then enable NightKeys.')
s=s.replace('Setup is complete. Tap Enable NightKeys to keep automatic lighting running.', 'Shizuku is connected. Enable NightKeys whenever you are ready.')
s=s.replace('Open or install Shizuku','Open Shizuku').replace('>Connect Shizuku</string>', '>Connect &amp; authorize</string>')
s=s.replace('Choose your keyboard brightness in the phone’s own settings. NightKeys keeps that level. The lowest visible level tested was 7; you do not need to enter a number here.', 'Optional: adjust keyboard brightness in the phone’s settings. NightKeys keeps your chosen level; there is nothing to capture or confirm.')
s=s.replace('</resources>', '''    <string name="step_install">1 · Install Shizuku</string>
    <string name="install_shizuku">Get Shizuku</string>
    <string name="install_detail">Shizuku lets NightKeys control the keyboard without root. Get it from the official download page, then return here.</string>
    <string name="shizuku_installed">✓ Already installed. You can skip this step.</string>
    <string name="install_first">Install Shizuku above to continue.</string>
    <string name="step_connect">2 · Connect &amp; authorize</string>
    <string name="start_shizuku">Start Shizuku</string>
    <string name="start_shizuku_detail">Open Shizuku and start its service. Return here, tap Connect &amp; authorize, and allow NightKeys in the prompt.</string>
    <string name="reconnect">Reconnect Shizuku</string>
    <string name="step_enable">3 · Enable NightKeys</string>
    <string name="enable_ready">Ready. Start automatic lighting now — no timed test or checklist.</string>
    <string name="enable_waiting">Connect Shizuku in step 2, then turn on automatic lighting here.</string>
    <string name="enabled_detail">Automatic lighting is on. It continues when you leave the app.</string>
    <string name="enable_note">Allow the quiet running notification if Android asks. Your stock keyboard brightness stays unchanged.</string>
    <string name="preferences_title">Optional preferences</string>
</resources>''');f.write_text(s)
(ROOT/'version.properties').write_text('versionName=1.0.0-beta.2\nversionCode=1000002\n')
f=ROOT/'core-tests/CoreTests.java';s=f.read_text();pos=s.index('public static void main(');start=s.index('{',pos)+1
s=s[:start]+'''
        for (int flags = 0; flags < 32; flags++) {
            boolean supported = (flags & 1) != 0, sensor = (flags & 2) != 0;
            boolean connected = (flags & 4) != 0, running = (flags & 8) != 0;
            boolean recovery = (flags & 16) != 0;
            boolean result = com.everyonesniche.nightkeys.core.Onboarding.canStart(
                    supported, sensor, connected, running, recovery);
            if (result != (flags == 7)) throw new AssertionError("Wrong setup gate: " + flags);
        }
        System.out.println("PASS  all 32 onboarding states; no consumer-test gate");
'''+s[start:];f.write_text(s)
f=ROOT/'tools/check-source.py';s=f.read_text();s+='''
# A completed trial/calibration must never become a prerequisite again.
for file in main.rglob('*.java'):
    text=file.read_text()
    assert not any(p in text for p in ('config.verified(', 'confirmStockMinimum', 'markVerified', 'testExpiry', 'LightingService.TEST')),file
for name in ('test', 'confirm_test', 'diag_hold', 'diag_force_off'):
    assert name not in ids, 'Consumer trial control leaked: '+name
print('PASS: no consumer test, calibration gate or 90-second expiry in the shipping app.')
''';f.write_text(s)
f=ROOT/'app/src/androidTest/java/com/everyonesniche/nightkeys/bridge/PermissionTests.java';s=f.read_text()
s=s.replace('    private void fresh() throws Exception {','''    private void fresh() throws Exception {
        test("fresh installation needs no saved check to enable on a compatible device", () -> {
            check(!context.getSharedPreferences("configuration", Context.MODE_PRIVATE).contains("verified_build"),
                    "Fresh fixture unexpectedly verified");
            check(com.everyonesniche.nightkeys.core.Onboarding.canStart(true, true, true, false, false),
                    "Fresh connected device was gated");
            check(!com.everyonesniche.nightkeys.core.Onboarding.canStart(true, true, true, false, true),
                    "Pending restoration must remain protected");
        });''')
s=s.replace('        test("production screen has no diagnostic hardware controls", () -> {','''        test("production screen has exactly three setup steps and no consumer test controls", () -> {
            int[] headings = {com.everyonesniche.nightkeys.R.string.step_install,
                    com.everyonesniche.nightkeys.R.string.step_connect,
                    com.everyonesniche.nightkeys.R.string.step_enable};
            for (int i = 0; i < headings.length; i++)
                check(context.getString(headings[i]).startsWith(Integer.toString(i+1)), "Wrong step order");
            check(context.getResources().getIdentifier("test", "id", context.getPackageName()) == 0, "Timed check remains");
            check(context.getResources().getIdentifier("confirm_test", "id", context.getPackageName()) == 0, "Confirmation remains");''');f.write_text(s)
f=ROOT/'tools/emulator-test.sh';s=f.read_text().replace('count=7 scenario=fresh','count=8 scenario=fresh');f.write_text(s)
print('Patched application setup; lighting policy / transport / journal unchanged.')
