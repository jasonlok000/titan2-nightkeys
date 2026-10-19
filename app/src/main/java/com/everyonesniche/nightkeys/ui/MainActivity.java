package com.everyonesniche.nightkeys.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.everyonesniche.nightkeys.BuildConfig;
import com.everyonesniche.nightkeys.Config;
import com.everyonesniche.nightkeys.DiagnosticLog;
import com.everyonesniche.nightkeys.NightKeysApp;
import com.everyonesniche.nightkeys.R;
import com.everyonesniche.nightkeys.RuntimeState;
import com.everyonesniche.nightkeys.core.DiagnosticSession;
import com.everyonesniche.nightkeys.hardware.AndroidPort;
import com.everyonesniche.nightkeys.service.DiagnosticMonitorService;
import com.everyonesniche.nightkeys.service.LightingService;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements SensorEventListener {
    private NightKeysApp app;
    private SensorManager sensors;
    private Sensor light;
    private boolean resumed, previewRegistered, dragging, updatingDiagnosticControls, diagnosticBusy;
    private float previewLux = Float.NaN;
    private String diagnosticReadout = "Live vendor values not refreshed yet.";
    private String diagnosticStatus = "";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable changed = this::render;
    private final Runnable countdown = new Runnable() {
        @Override public void run() { if (resumed) { render(); main.postDelayed(this,1000); } }
    };
    private TextView statusTitle,statusDetail,luxValue,thresholdLabel,accessLabel,minimumLabel,error,diagLive,diagLog;
    private Button primary,capture,test,confirm,diagForceOff,diagRestore,diagLifecycle,diagSensor,diagStopMonitor;
    private SeekBar threshold;
    private LuxMeterView meter;
    private Switch diagHold;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); app=(NightKeysApp)getApplication();
        getWindow().setDecorFitsSystemWindows(false); setContentView(R.layout.activity_main);
        View root=findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        root.requestApplyInsets();
        sensors=getSystemService(SensorManager.class); light=sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        statusTitle=findViewById(R.id.status_title); statusDetail=findViewById(R.id.status_detail);
        luxValue=findViewById(R.id.lux_value); thresholdLabel=findViewById(R.id.threshold_label);
        accessLabel=findViewById(R.id.access_label); minimumLabel=findViewById(R.id.minimum_label);
        error=findViewById(R.id.error); primary=findViewById(R.id.main_action);
        capture=findViewById(R.id.capture); test=findViewById(R.id.test); confirm=findViewById(R.id.confirm_test);
        meter=findViewById(R.id.lux_meter); threshold=findViewById(R.id.threshold);
        diagLive=findViewById(R.id.diag_live); diagLog=findViewById(R.id.diag_log); diagHold=findViewById(R.id.diag_hold);
        diagForceOff=findViewById(R.id.diag_force_off); diagRestore=findViewById(R.id.diag_restore);
        diagLifecycle=findViewById(R.id.diag_lifecycle); diagSensor=findViewById(R.id.diag_sensor);
        diagStopMonitor=findViewById(R.id.diag_stop_monitor);
        ((TextView)findViewById(R.id.device_label)).setText(Build.MODEL+" · "+Build.DISPLAY);

        primary.setOnClickListener(v->{
            if (diagnosticHardwarePending()) restoreDiagnostic();
            else if (app.state.running || app.journal.pending()) command(LightingService.STOP);
            else requestVisibleService(LightingService.START);
        });
        findViewById(R.id.authorize).setOnClickListener(v->app.bridge.authorize());
        findViewById(R.id.open_display).setOnClickListener(v->open(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));

        diagHold.setOnCheckedChangeListener((button,checked)->{
            if (updatingDiagnosticControls) return;
            if (checked) {
                new AlertDialog.Builder(this).setTitle(R.string.diag_hold_confirm_title)
                        .setMessage(R.string.diag_hold_confirm).setNegativeButton(R.string.cancel,(d,w)->render())
                        .setPositiveButton("Start isolated hold",(d,w)->startDiagnosticHold()).show();
            } else if ("hold".equals(app.config.diagnosticMode()) && app.journal.pending()) {
                restoreDiagnostic();
            } else render();
        });
        findViewById(R.id.diag_refresh).setOnClickListener(v->refreshDiagnosticReadout());
        diagForceOff.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(R.string.diag_off_confirm_title)
                .setMessage(R.string.diag_off_confirm).setNegativeButton(R.string.cancel,null)
                .setPositiveButton("Force off",(d,w)->startDiagnosticOff()).show());
        diagRestore.setOnClickListener(v->restoreDiagnostic());
        diagLifecycle.setOnClickListener(v->requestDiagnosticMonitor(DiagnosticMonitorService.START_LIFECYCLE));
        diagSensor.setOnClickListener(v->requestDiagnosticMonitor(DiagnosticMonitorService.START_SENSOR));
        diagStopMonitor.setOnClickListener(v->{ DiagnosticMonitorService.command(this,DiagnosticMonitorService.STOP); main.postDelayed(this::render,250); });
        findViewById(R.id.diag_clear_log).setOnClickListener(v->{ app.diagnostics.clear(); diagnosticStatus="Diagnostic event log cleared."; render(); });

        capture.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(R.string.capture_title)
                .setMessage(R.string.capture_message).setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.capture_confirm,(d,which)->captureMinimum()).show());
        test.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(R.string.test_notice_title)
                .setMessage(R.string.test_notice).setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.begin_test,(d,which)->requestVisibleService(LightingService.TEST)).show());
        confirm.setOnClickListener(v->confirmPhysicalChecks());

        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { dragging=true; }
            @Override public void onProgressChanged(SeekBar s,int progress,boolean user) {
                if (user) {
                    showThreshold((progress+1)/2f);
                    if (!dragging) { main.removeCallbacks(accessibleThresholdCommit); main.postDelayed(accessibleThresholdCommit,180); }
                }
            }
            @Override public void onStopTrackingTouch(SeekBar s) { dragging=false; saveThreshold((s.getProgress()+1)/2f); }
        });
        threshold.setOnKeyListener((view,key,event)->{
            if (event.getAction()==android.view.KeyEvent.ACTION_UP) main.post(()->saveThreshold((threshold.getProgress()+1)/2f));
            return false;
        });
        findViewById(R.id.room_threshold).setOnClickListener(v->{
            float value=currentLux();
            if (!Float.isFinite(value)) { toast("Wait for a valid ambient-light reading."); return; }
            saveThreshold(Math.max(0.5f,Math.min(100f,(float)Math.ceil((value+1)*2)/2)));
        });
        findViewById(R.id.advanced_toggle).setOnClickListener(v->{
            View details=findViewById(R.id.advanced_body);
            details.setVisibility(details.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        Switch boot=findViewById(R.id.resume_boot); boot.setChecked(app.config.resumeAfterBoot());
        boot.setOnCheckedChangeListener((v,checked)->{ try { app.config.resumeAfterBoot(checked); } catch(Exception e){ showError(e); } });
        findViewById(R.id.app_settings).setOnClickListener(v->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        findViewById(R.id.diagnostics).setOnClickListener(v->copyDiagnostics());
        findViewById(R.id.restore).setOnClickListener(v->{ if (diagnosticHardwarePending()) restoreDiagnostic(); else command(LightingService.STOP); });
        if (saved != null) {
            if (saved.getBoolean("advanced")) findViewById(R.id.advanced_body).setVisibility(View.VISIBLE);
            pendingCommand = saved.getString("pending_command");
        }
    }

    @Override protected void onResume() {
        super.onResume(); resumed=true; app.addListener(changed); app.bridge.connect(); render(); main.post(countdown);
        main.postDelayed(this::refreshDiagnosticReadout,300);
    }
    @Override protected void onPause() {
        resumed=false; app.removeListener(changed); main.removeCallbacks(countdown); stopPreview(); super.onPause();
    }
    @Override protected void onDestroy() { main.removeCallbacksAndMessages(null); io.shutdown(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("advanced",findViewById(R.id.advanced_body).getVisibility()==View.VISIBLE);
        out.putString("pending_command",pendingCommand); super.onSaveInstanceState(out);
    }

    private void render() {
        if (isFinishing() || isDestroyed()) return;
        RuntimeState state=app.state;
        statusTitle.setText(state.title);
        String detail=state.detail;
        if (state.testing) detail += " · " + Math.max(0,(state.testEndMs-SystemClock.elapsedRealtime()+999)/1000)+"s left";
        statusDetail.setText(detail);

        boolean pending=app.journal.pending();
        boolean diagnosticPending=diagnosticHardwarePending();
        boolean monitor=app.diagnostics.monitorRunning();
        boolean active=state.running || pending;
        if (diagnosticPending) primary.setText("Restore isolated diagnostic change");
        else primary.setText(active ? (state.testing ? "End check & restore" : "Stop & restore") : getString(R.string.enable));
        enabled(primary,diagnosticBusy ? false : (active || (!monitor && app.bridge.ready() && app.config.verified() && Config.supportedIdentity())));
        enabled(capture,!active && !monitor && Config.supportedIdentity());
        enabled(test,app.bridge.ready() && !active && !monitor && app.config.captured() && Config.supportedIdentity());
        enabled(findViewById(R.id.open_display),!active && !monitor);
        enabled(findViewById(R.id.restore),active && app.bridge.ready() && !diagnosticBusy);
        confirm.setVisibility(app.config.hasTest() && !app.config.verified() ? View.VISIBLE : View.GONE);
        enabled(confirm,!active && !monitor);

        String bridgeStatus=app.bridge.status();
        if (!app.bridge.ready() && app.bridge.authorized())
            bridgeStatus += "\nThe sensor/lifecycle monitor still works without vendor access; hardware tests require agui_common.";
        accessLabel.setText(bridgeStatus);
        if (app.config.captured()) {
            minimumLabel.setText("Stock minimum confirmed for this firmware"
                    +(app.config.verified() ? "\nPhysical checks confirmed." : "\nPhysical checks still required."));
        } else minimumLabel.setText(getString(R.string.minimum_initial));

        renderDiagnosticControls(state, pending, monitor);

        if (!dragging) { threshold.setProgress(Math.round(app.config.threshold()*2)-1); showThreshold(app.config.threshold()); }
        String issue=app.config.lastError();
        if (!Config.supportedIdentity()) issue="Device identity not recognized as Titan 2 Elite in the owner profile. Hardware writes are disabled.";
        else if (light==null) issue="No ambient-light sensor was reported by Android.";
        error.setText(issue); error.setVisibility(issue.isEmpty()?View.GONE:View.VISIBLE);
        if (resumed && !state.running && !monitor && !previewRegistered && light!=null) {
            previewRegistered=sensors.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL);
        } else if (state.running || monitor) stopPreview();
        showLux(currentLux());
    }

    private void renderDiagnosticControls(RuntimeState state, boolean pending, boolean monitor) {
        String mode=app.config.diagnosticMode();
        boolean holdActive=pending && "hold".equals(mode);
        boolean offActive=pending && "off".equals(mode);
        updatingDiagnosticControls=true;
        diagHold.setChecked(holdActive);
        updatingDiagnosticControls=false;
        enabled(diagHold,!diagnosticBusy && !state.running && !monitor && (holdActive || (!pending && app.bridge.ready())));
        enabled(diagForceOff,!diagnosticBusy && !state.running && !monitor && !pending && app.bridge.ready());
        enabled(diagRestore,!diagnosticBusy && !state.running && pending && app.bridge.ready());
        enabled(findViewById(R.id.diag_refresh),!diagnosticBusy);
        enabled(diagLifecycle,!diagnosticBusy && !state.running && !pending && !monitor);
        enabled(diagSensor,!diagnosticBusy && !state.running && !pending && !monitor && light!=null);
        enabled(diagStopMonitor,monitor);
        StringBuilder live=new StringBuilder();
        if (!diagnosticStatus.isEmpty()) live.append(diagnosticStatus).append('\n');
        live.append(diagnosticReadout)
                .append("\nHardware test mode: ").append(mode)
                .append(" · recovery pending: ").append(pending)
                .append("\nMonitor: ").append(monitor ? app.diagnostics.monitorMode()+" · pid="+app.diagnostics.monitorPid()+" · generation="+app.diagnostics.monitorGeneration() : "stopped");
        if (offActive) live.append("\nForced-OFF test is active; restore before starting another test.");
        diagLive.setText(live.toString());
        diagLog.setText(app.diagnostics.recent(18));
    }

    private boolean diagnosticHardwarePending() {
        return app.journal.pending() && !"none".equals(app.config.diagnosticMode());
    }

    private void startDiagnosticHold() {
        runDiagnosticMutation("Applying isolated continuous timeout…", () -> {
            ensureHardwareDiagnosticCanStart();
            DiagnosticSession session=new DiagnosticSession(new AndroidPort(this,app.bridge),app.journal);
            String original=session.startContinuousHold();
            app.config.diagnosticMode("hold");
            app.diagnostics.append("HARDWARE_HOLD_START original_timeout="+original
                    +" target=2147483647 · no sensor/service/screen handler");
            return "Manual hold active. NightKeys will not touch the vendor timeout until you restore it.";
        });
    }

    private void startDiagnosticOff() {
        runDiagnosticMutation("Applying isolated forced-OFF…", () -> {
            ensureHardwareDiagnosticCanStart();
            DiagnosticSession session=new DiagnosticSession(new AndroidPort(this,app.bridge),app.journal);
            String original=session.startForcedOff();
            app.config.diagnosticMode("off");
            app.diagnostics.append("HARDWARE_FORCE_OFF original_timeout="+original+" target=0 + close broadcast");
            return "Forced-OFF test active. Restore the saved original when finished.";
        });
    }

    private void restoreDiagnostic() {
        runDiagnosticMutation("Restoring saved original timeout…", () -> {
            if (app.state.running) throw new IllegalStateException("Stop automatic lighting before diagnostic restore");
            if (!app.bridge.ready()) throw new IllegalStateException("Reconnect Shizuku / agui_common before restoring");
            DiagnosticSession session=new DiagnosticSession(new AndroidPort(this,app.bridge),app.journal);
            String restored=session.restore();
            app.config.diagnosticMode("none");
            app.diagnostics.append("HARDWARE_RESTORE completed timeout="+(restored==null?"no pending snapshot":restored));
            return restored==null ? "No diagnostic hardware change was pending." : "Restored original timeout: "+restored;
        });
    }

    private interface DiagnosticAction { String run() throws Exception; }
    private void runDiagnosticMutation(String progress, DiagnosticAction action) {
        if (diagnosticBusy) return;
        diagnosticBusy=true; diagnosticStatus=progress; render();
        io.execute(()->{
            String result;
            Exception failure=null;
            try { result=action.run(); app.config.error(""); }
            catch(Exception e) { failure=e; result=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(); app.config.error(result); app.diagnostics.append("DIAGNOSTIC_MUTATION_FAILED "+result); }
            String finalResult=result; Exception finalFailure=failure;
            main.post(()->{
                diagnosticBusy=false; diagnosticStatus=finalResult;
                refreshDiagnosticReadout();
                render();
                if (finalFailure!=null) toast(finalResult); else toast(finalResult);
            });
        });
    }

    private void ensureHardwareDiagnosticCanStart() {
        if (app.state.running) throw new IllegalStateException("Stop automatic lighting first");
        if (app.diagnostics.monitorRunning()) throw new IllegalStateException("Stop the diagnostic monitor first");
        if (app.journal.pending()) throw new IllegalStateException("Restore the existing hardware change first");
        if (!app.bridge.ready()) throw new IllegalStateException("Authorize Shizuku and wait for agui_common ready");
        if (!Config.supportedIdentity()) throw new IllegalStateException("Unsupported device identity");
    }

    private void refreshDiagnosticReadout() {
        if (diagnosticBusy || isDestroyed()) return;
        io.execute(()->{
            String text;
            try {
                StringBuilder out=new StringBuilder()
                        .append("App pid=").append(Process.myPid())
                        .append(" · screen interactive=").append(AndroidPort.screenOn(this));
                if (app.bridge.ready()) {
                    Bundle vendor=app.bridge.require().inspect();
                    out.append("\nLive timeout=").append(vendor.getString("timeout"))
                            .append(" · backup=").append(vendor.getString("timeoutBackup"))
                            .append(" · brightness=").append(vendor.getString("brightness"))
                            .append(" · auto=").append(vendor.getString("auto"))
                            .append("\nPrivileged shell UID=").append(vendor.getInt("shellUid"));
                } else out.append("\nVendor read unavailable: ").append(app.bridge.status());
                float monitored=app.diagnostics.latestSensorLux();
                if (Float.isFinite(monitored)) out.append("\nLatest monitor sensor=").append(monitored).append(" lux");
                text=out.toString();
            } catch(Exception e) { text="Live vendor read failed: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()); }
            String finalText=text;
            main.post(()->{ if (!isDestroyed()) { diagnosticReadout=finalText; render(); } });
        });
    }

    private void requestDiagnosticMonitor(String action) {
        if (app.state.running || app.journal.pending()) { toast("Stop/restore hardware changes before starting a non-invasive monitor."); return; }
        requestVisibleService("D:"+action);
    }

    private void enabled(View view,boolean yes) { view.setEnabled(yes); view.setAlpha(yes?1f:0.42f); }
    private void showThreshold(float value) {
        float off=Math.max(value+3,value*1.5f);
        thresholdLabel.setText(String.format(Locale.getDefault(),"On below %.1f lux · off at %.1f lux",value,off));
        threshold.setContentDescription(String.format(Locale.getDefault(),"Darkness threshold: %.1f lux",value));
        meter.readings(currentLux(),value);
    }
    private final Runnable accessibleThresholdCommit = () -> saveThreshold((threshold.getProgress()+1)/2f);
    private void saveThreshold(float value) {
        if (Float.compare(value,app.config.threshold()) == 0) return;
        try {
            app.config.threshold(value); if (app.state.running) command(LightingService.REFRESH); render();
        } catch(Exception e) { showError(e); }
    }
    private float currentLux() {
        if (app.state.running) return app.state.lux;
        if (app.diagnostics.monitorRunning() && DiagnosticLog.MODE_SENSOR.equals(app.diagnostics.monitorMode())) {
            float monitored=app.diagnostics.latestSensorLux();
            if (Float.isFinite(monitored)) return monitored;
        }
        return previewLux;
    }
    private void showLux(float value) {
        String text=!Float.isFinite(value)?"—":value>=100?String.format(Locale.getDefault(),"%.0f",value):String.format(Locale.getDefault(),"%.1f",value);
        luxValue.setText(text); luxValue.setContentDescription("Ambient light: "+text+" lux");
        meter.readings(value,app.config.threshold());
    }
    private void stopPreview() {
        if (previewRegistered) sensors.unregisterListener(this);
        previewRegistered=false;
    }
    @Override public void onSensorChanged(SensorEvent e) {
        float v=e.values.length>0?e.values[0]:Float.NaN;
        previewLux=Float.isFinite(v)&&v>=0?v:Float.NaN;
        if (!app.state.running) showLux(previewLux);
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { }

    private void captureMinimum() {
        if (app.state.running || app.journal.pending()) { toast("Stop and restore before calibration."); return; }
        try {
            if (!Config.supportedIdentity()) throw new IllegalStateException("Unsupported device identity");
            app.config.confirmStockMinimum(); app.config.error("");
            toast("Stock minimum confirmed. The diagnostic lab does not need this calibration."); render();
        } catch(Exception e) { showError(e); }
    }

    private void confirmPhysicalChecks() {
        if (app.state.running || app.journal.pending()) { toast("Finish the check and restore settings first."); return; }
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*getResources().getDisplayMetrics().density); body.setPadding(pad,pad,pad,pad);
        int[] labels={R.string.test_check_min,R.string.test_check_stays,R.string.test_check_screen,R.string.test_check_bright};
        CheckBox[] checks=new CheckBox[labels.length];
        for(int i=0;i<labels.length;i++) {
            checks[i]=new CheckBox(this); checks[i].setText(labels[i]); checks[i].setMinHeight((int)(56*getResources().getDisplayMetrics().density)); body.addView(checks[i]);
        }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.test_dialog_title).setView(body)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.verified,null).create();
        dialog.setOnShowListener(d->{
            Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE); save.setEnabled(false);
            for(CheckBox check:checks) check.setOnCheckedChangeListener((b,checked)->{
                boolean all=true;for(CheckBox other:checks)all&=other.isChecked();save.setEnabled(all);
            });
            save.setOnClickListener(v->{
                try { app.config.markVerified();dialog.dismiss();render();toast("Setup saved. Automatic lighting is ready."); }
                catch(Exception e){ showError(e); }
            });
        });dialog.show();
    }

    private String pendingCommand;
    private void requestVisibleService(String action) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            pendingCommand=action;
            new AlertDialog.Builder(this).setTitle(R.string.permission_title).setMessage(R.string.permission_message)
                    .setNegativeButton(R.string.cancel,(d,w)->pendingCommand=null)
                    .setPositiveButton(R.string.continue_label,(d,w)->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},421)).show();
        } else dispatchServiceAction(action);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==421) {
            String action=pendingCommand;pendingCommand=null;
            if(grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED && action!=null) dispatchServiceAction(action);
            else toast("Enable NightKeys notifications in system settings before starting a foreground mode.");
        }
    }
    private void dispatchServiceAction(String action) {
        try {
            if (action != null && action.startsWith("D:")) DiagnosticMonitorService.command(this,action.substring(2));
            else LightingService.command(this,action);
            main.postDelayed(this::render,250);
        } catch(Exception e){ showError(e); }
    }
    private void command(String action) { dispatchServiceAction(action); }
    private void open(Intent intent) { try { startActivity(intent); } catch(Exception e){ showError(e); } }
    private void toast(String text) { if(!isFinishing()) Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    private void showError(Exception e) {
        String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
        app.config.error(message); toast(message); render();
    }

    private void copyDiagnostics() {
        io.execute(()->{
            StringBuilder out=new StringBuilder("NightKeys "+BuildConfig.VERSION_NAME+" diagnostics\n")
                .append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("\nBuild: ").append(Build.DISPLAY).append("\nFingerprint: ").append(Build.FINGERPRINT)
                .append("\nSupported identity: ").append(Config.supportedIdentity())
                .append("\nApp PID: ").append(Process.myPid())
                .append("\nScreen interactive: ").append(AndroidPort.screenOn(this))
                .append("\nShizuku authorized: ").append(app.bridge.authorized())
                .append("\nagui_common channel ready: ").append(app.bridge.ready())
                .append("\nShizuku server UID: ").append(app.bridge.authorized() ? rikka.shizuku.Shizuku.getUid() : -1)
                .append("\nShizuku API: ").append(app.bridge.authorized() ? rikka.shizuku.Shizuku.getVersion() : -1)
                .append("\nShizuku: ").append(app.bridge.status())
                .append("\nTransport: ").append(app.bridge.transportDiagnostics())
                .append("\nSensor: ").append(light==null?"absent":light.getName())
                .append("\nAmbient lux: ").append(currentLux())
                .append("\nThresholds: ").append(app.config.threshold()).append(" / ").append(app.config.offThreshold())
                .append("\nStock minimum confirmed: ").append(app.config.captured())
                .append("\nUser-verified firmware: ").append(app.config.verified())
                .append("\nAutomation requested: ").append(app.config.wanted())
                .append("\nDiagnostic hardware mode: ").append(app.config.diagnosticMode())
                .append("\nDiagnostic monitor running: ").append(app.diagnostics.monitorRunning())
                .append("\nDiagnostic monitor mode: ").append(app.diagnostics.monitorMode())
                .append("\nDiagnostic monitor PID/generation: ").append(app.diagnostics.monitorPid()).append('/').append(app.diagnostics.monitorGeneration())
                .append("\nStatus: ").append(app.state.title).append(" / ").append(app.state.detail);
            try {
                Bundle vendor=app.bridge.require().inspect();
                out.append("\nVendor keyboard_brightness_timeout=").append(vendor.getString("timeout"))
                    .append("\nVendor keyboard_brightness_timeout_backup=").append(vendor.getString("timeoutBackup"))
                    .append("\nVendor keyboard_led_brightness=").append(vendor.getString("brightness"))
                    .append("\nVendor keyboard_led_auto_switch=").append(vendor.getString("auto"))
                    .append("\nPrivileged shell UID=").append(vendor.getInt("shellUid"))
                    .append("\nVendor transport=").append(vendor.getString("transport"));
            } catch(Exception e){out.append("\nVendor diagnostics unavailable: ").append(e.getMessage());}
            out.append("\nRecovery journal: ").append(app.journal.export())
                .append("\nLast error: ").append(app.config.lastError())
                .append("\nDiagnostic event log:\n").append(app.diagnostics.export())
                .append("\nNo LED luminance measurement is available. Requested settings are not physical verification.\n");
            main.post(()->{
                if(isDestroyed())return;
                getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("NightKeys diagnostics",out.toString()));
                toast("Diagnostic report copied. It contains device/build, NightKeys settings and the local diagnostic event log.");
            });
        });
    }
}
