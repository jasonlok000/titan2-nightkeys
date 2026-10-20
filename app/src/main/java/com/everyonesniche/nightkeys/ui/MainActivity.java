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
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.everyonesniche.nightkeys.BuildConfig;
import com.everyonesniche.nightkeys.Config;
import com.everyonesniche.nightkeys.NightKeysApp;
import com.everyonesniche.nightkeys.R;
import com.everyonesniche.nightkeys.RuntimeState;
import com.everyonesniche.nightkeys.core.Onboarding;
import rikka.shizuku.Shizuku;
import com.everyonesniche.nightkeys.hardware.AndroidPort;
import com.everyonesniche.nightkeys.service.LightingService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Community UI. Read-only support export; no manual hardware overrides or lab services. */
public final class MainActivity extends Activity implements SensorEventListener {
    private static final String GUIDE = "https://jasonlok000.github.io/titan2-nightkeys/setup.html";
    private static final int NOTIFICATIONS = 101;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable changed = this::render;
    private NightKeysApp app;
    private SensorManager sensors;
    private Sensor light;
    private boolean resumed, previewRegistered, dragging;
    private float previewLux = Float.NaN;
    private String pendingAction;
    private TextView statusTitle, statusDetail, luxValue, thresholdLabel, accessLabel, error, installLabel, enableDetail;
    private Button primary, install, authorize;
    private SeekBar threshold;
    private LuxMeterView meter;
    private final Runnable uiRefresh = new Runnable() {
        @Override public void run() {
            if (resumed) { render(); main.postDelayed(this, 1000); }
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        app = (NightKeysApp) getApplication();
        if (saved != null) pendingAction = saved.getString("pendingAction");
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
        sensors = getSystemService(SensorManager.class);
        light = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        statusTitle = findViewById(R.id.status_title);
        statusDetail = findViewById(R.id.status_detail);
        luxValue = findViewById(R.id.lux_value);
        thresholdLabel = findViewById(R.id.threshold_label);
        accessLabel = findViewById(R.id.access_label);
        error = findViewById(R.id.error);
        primary = findViewById(R.id.main_action);
        install = findViewById(R.id.install_shizuku);
        authorize = findViewById(R.id.authorize);
        installLabel = findViewById(R.id.install_label);
        enableDetail = findViewById(R.id.enable_detail);
        threshold = findViewById(R.id.threshold);
        meter = findViewById(R.id.lux_meter);
        ((TextView) findViewById(R.id.version)).setText(getString(R.string.version_label, BuildConfig.VERSION_NAME));
        primary.setOnClickListener(v -> {
            if (app.state.running || app.journal.pending()) command(LightingService.STOP);
            else requestVisibleService(LightingService.START);
        });
        install.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/"))));
        authorize.setOnClickListener(v -> {
            if (!shizukuInstalled() || !shizukuRunning()) openShizuku();
            else app.bridge.authorize();
        });
        findViewById(R.id.open_shizuku).setOnClickListener(v -> openShizuku());
        findViewById(R.id.open_keyboard).setOnClickListener(v -> openKeyboardSettings());
        findViewById(R.id.guide).setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse(GUIDE))));
        findViewById(R.id.battery).setOnClickListener(v -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.support).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.report_title).setMessage(R.string.report_consent)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.copy_report, (d, w) -> copyReport()).show());
        findViewById(R.id.licenses).setOnClickListener(v -> showLicenses());
        CheckBox resume = findViewById(R.id.resume_boot);
        resume.setChecked(app.config.resumeAfterBoot());
        resume.setOnCheckedChangeListener((v, checked) -> app.config.resumeAfterBoot(checked));
        threshold.setMax(199);
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) { dragging = true; }
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) showThreshold((progress + 1) / 2f);
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                app.config.threshold((bar.getProgress() + 1) / 2f);
                if (app.state.running) command(LightingService.REFRESH);
                render();
            }
        });
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("pendingAction", pendingAction);
        super.onSaveInstanceState(out);
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; app.addListener(changed);
        app.bridge.connect(); render(); uiRefresh.run();
    }
    @Override protected void onPause() {
        resumed = false; main.removeCallbacks(uiRefresh); app.removeListener(changed);
        unregisterPreview(); super.onPause();
    }
    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null); io.shutdown(); super.onDestroy();
    }
    private void render() {
        if (isFinishing() || isDestroyed()) return;
        RuntimeState state = app.state;
        boolean active = state.running;
        boolean pending = app.journal.pending();
        boolean supported = Config.supportedIdentity() && light != null;
        boolean installed = shizukuInstalled();
        boolean server = shizukuRunning();
        if (!supported) {
            statusTitle.setText(R.string.unsupported_title);
            statusDetail.setText(R.string.unsupported_detail);
        } else if (!active && pending) {
            statusTitle.setText(R.string.restore_title);
            statusDetail.setText(R.string.restore_detail);
        } else if (!active && app.bridge.ready()) {
            statusTitle.setText(R.string.ready_title);
            statusDetail.setText(R.string.ready_detail);
        } else if (!active) {
            statusTitle.setText(R.string.welcome_title);
            statusDetail.setText(R.string.welcome_detail);
        } else {
            statusTitle.setText(state.title);
            statusDetail.setText(state.detail);
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
        // Reconnection must remain available if a session needs restoration.
        authorize.setEnabled(installed);
        findViewById(R.id.open_shizuku).setVisibility(installed ? View.VISIBLE : View.GONE);
        enableDetail.setText(active ? R.string.enabled_detail : pending ? R.string.restore_detail
                : app.bridge.ready() ? R.string.enable_ready : R.string.enable_waiting);

        error.setText(app.config.lastError());
        error.setVisibility(app.config.lastError().isBlank() ? View.GONE : View.VISIBLE);
        if (!dragging) {
            threshold.setProgress(Math.max(0, Math.round(app.config.threshold() * 2) - 1));
            showThreshold(app.config.threshold());
        }
        updatePreviewRegistration();
        float lux = active ? state.lux : previewLux;
        luxValue.setText(Float.isFinite(lux) ? String.format(Locale.getDefault(), "%.1f lux", lux) : getString(R.string.unknown_lux));
        meter.readings(lux, app.config.threshold());
    }
    private void showThreshold(float on) {
        float off = Math.max(on + 3f, on * 1.5f);
        thresholdLabel.setText(getString(R.string.threshold_format, on, off));
    }
    private void updatePreviewRegistration() {
        boolean need = resumed && !app.state.running && light != null;
        if (need && !previewRegistered) previewRegistered = sensors.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        else if (!need) unregisterPreview();
    }
    private void unregisterPreview() {
        if (previewRegistered && sensors != null) sensors.unregisterListener(this);
        previewRegistered = false;
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.values.length > 0) { previewLux = event.values[0]; render(); }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    private void requestVisibleService(String action) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = action;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS);
        } else command(action);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != NOTIFICATIONS) return;
        String action = pendingAction; pendingAction = null;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED && action != null) command(action);
        else toast(getString(R.string.notification_needed));
    }
    private void command(String action) {
        try { LightingService.command(this, action); main.postDelayed(this::render, 150); }
        catch (RuntimeException e) { app.config.error(e.getMessage()); render(); }
    }
    private boolean shizukuInstalled() {
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
    private void openKeyboardSettings() {
        try { startActivity(new Intent().setClassName("com.agui.settings", "com.agui.settings.touchpad.KeyboardLEDSettingsActivity")); }
        catch (RuntimeException e) { open(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); }
    }
    private void copyReport() {
        final String initial = "NightKeys " + BuildConfig.VERSION_NAME + " support report\n"
                + "Model: " + Build.MANUFACTURER + " " + Build.MODEL + "\nFirmware: " + Build.DISPLAY
                + "\nAndroid: " + Build.VERSION.RELEASE + "\nSupported identity: " + Config.supportedIdentity()
                + "\nScreen on: " + AndroidPort.screenOn(this) + "\nShizuku: " + app.bridge.status()
                + "\nSensor: " + (light == null ? "none" : light.getName())
                + "\nLux: " + (app.state.running ? app.state.lux : previewLux)
                + "\nThresholds: " + app.config.threshold() + " / " + app.config.offThreshold()
                + "\nAutomation requested: " + app.config.wanted()
                + "\nService running: " + app.state.running + "\nState: " + app.state.title + " / " + app.state.detail
                + "\nRecovery: " + app.journal.export() + "\nLast error: " + app.config.lastError() + "\n";
        io.execute(() -> {
            StringBuilder report = new StringBuilder(initial);
            try {
                Bundle values = app.bridge.require().inspect();
                report.append("Live timeout: ").append(values.getString("timeout"))
                        .append("\nStock tile backup: ").append(values.getString("timeoutBackup"))
                        .append("\nStock brightness: ").append(values.getString("brightness"))
                        .append("\nPrivileged UID: ").append(values.getInt("shellUid"));
            } catch (Exception e) { report.append("Live read unavailable: ").append(e.getMessage()); }
            report.append("\nReadbacks are settings, not measured LED brightness.\n");
            main.post(() -> {
                if (isDestroyed()) return;
                getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("NightKeys support report", report.toString()));
                toast(getString(R.string.report_copied));
            });
        });
    }
    private void showLicenses() {
        try (InputStream input = getAssets().open("THIRD_PARTY_NOTICES.txt")) {
            TextView text = new TextView(this); text.setText(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            text.setTextIsSelectable(true); text.setTextSize(14); text.setPadding(24, 16, 24, 16);
            ScrollView scroll = new ScrollView(this); scroll.addView(text);
            new AlertDialog.Builder(this).setTitle(R.string.licenses).setView(scroll).setPositiveButton(android.R.string.ok, null).show();
        } catch (Exception e) { toast(getString(R.string.license_error)); }
    }
    private void open(Intent intent) {
        try { startActivity(intent); } catch (RuntimeException e) { toast(getString(R.string.open_failed)); }
    }
    private void toast(String message) { Toast.makeText(this, message == null ? getString(R.string.action_failed) : message, Toast.LENGTH_LONG).show(); }
}
