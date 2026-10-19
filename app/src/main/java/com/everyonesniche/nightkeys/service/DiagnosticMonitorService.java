package com.everyonesniche.nightkeys.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.view.Display;
import com.everyonesniche.nightkeys.DiagnosticLog;
import com.everyonesniche.nightkeys.NightKeysApp;
import com.everyonesniche.nightkeys.R;
import com.everyonesniche.nightkeys.hardware.AndroidPort;
import com.everyonesniche.nightkeys.ui.MainActivity;
import java.util.Locale;

/**
 * Diagnostic-only foreground monitor. It never calls BridgeClient, never changes a vendor
 * timeout, and never broadcasts a keyboard-light command. Its only jobs are to record the
 * Android service lifecycle and, in sensor mode, prove whether a fresh ALS event arrives
 * after a screen-off/on cycle.
 */
public final class DiagnosticMonitorService extends Service implements SensorEventListener {
    public static final String START_LIFECYCLE = "com.everyonesniche.nightkeys.DIAG_LIFECYCLE";
    public static final String START_SENSOR = "com.everyonesniche.nightkeys.DIAG_SENSOR";
    public static final String STOP = "com.everyonesniche.nightkeys.DIAG_STOP";
    private static final String CHANNEL = "nightkeys_diagnostic_monitor";
    private static final int NOTIFICATION_ID = 205;

    private NightKeysApp app;
    private DiagnosticLog log;
    private HandlerThread thread;
    private Handler worker;
    private SensorManager sensors;
    private DisplayManager displays;
    private Sensor light;
    private boolean sensorRegistered;
    private String mode = DiagnosticLog.MODE_NONE;
    private int generation;
    private long lastScreenOnElapsed = -1;
    private long lastSensorLogElapsed = -1;
    private float lastLoggedLux = Float.NaN;
    private boolean waitingFirstSample;
    private boolean intentionalStop;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            worker.post(() -> handleScreenEvent(intent.getAction()));
        }
    };

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int displayId) { }
        @Override public void onDisplayRemoved(int displayId) {
            if (displayId == Display.DEFAULT_DISPLAY) record("DISPLAY_REMOVED default");
        }
        @Override public void onDisplayChanged(int displayId) {
            if (displayId == Display.DEFAULT_DISPLAY)
                record("DISPLAY_CHANGED default · interactive=" + AndroidPort.screenOn(DiagnosticMonitorService.this));
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        app = (NightKeysApp)getApplication();
        log = app.diagnostics;
        generation = log.nextMonitorGeneration();
        startForeground(NOTIFICATION_ID, notification("Starting diagnostic monitor…"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        thread = new HandlerThread("nightkeys-diagnostic-monitor");
        thread.start();
        worker = new Handler(thread.getLooper());
        sensors = getSystemService(SensorManager.class);
        displays = getSystemService(DisplayManager.class);
        light = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        if (displays != null) displays.registerDisplayListener(displayListener, worker);
        record("MONITOR_SERVICE_CREATE generation=" + generation + " pid=" + Process.myPid()
                + " · interactive=" + AndroidPort.screenOn(this));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        worker.post(() -> {
            if (STOP.equals(action)) {
                intentionalStop = true;
                record("MONITOR_STOP_REQUEST");
                stopSensor();
                log.setMonitorState(false, DiagnosticLog.MODE_NONE, -1);
                app.notifyChanged();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            if (START_SENSOR.equals(action)) mode = DiagnosticLog.MODE_SENSOR;
            else mode = DiagnosticLog.MODE_LIFECYCLE;
            log.setMonitorState(true, mode, Process.myPid());
            record("MONITOR_START mode=" + mode + " startId=" + startId
                    + " · interactive=" + AndroidPort.screenOn(this));
            if (DiagnosticLog.MODE_SENSOR.equals(mode)) {
                if (light == null) record("SENSOR_ABSENT");
                else if (AndroidPort.screenOn(this)) startSensor("service start");
            } else stopSensor();
            refreshNotification();
            app.notifyChanged();
        });
        return START_NOT_STICKY;
    }

    private void handleScreenEvent(String action) {
        boolean interactive = AndroidPort.screenOn(this);
        record((Intent.ACTION_SCREEN_ON.equals(action) ? "SCREEN_ON" : "SCREEN_OFF")
                + " broadcast · interactive=" + interactive);
        if (!DiagnosticLog.MODE_SENSOR.equals(mode)) return;
        if (Intent.ACTION_SCREEN_OFF.equals(action) || !interactive) {
            stopSensor();
        } else {
            lastScreenOnElapsed = SystemClock.elapsedRealtime();
            waitingFirstSample = true;
            startSensor("screen wake");
        }
        refreshNotification();
    }

    private void startSensor(String reason) {
        if (sensorRegistered || sensors == null || light == null) return;
        sensorRegistered = sensors.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL, 0, worker);
        waitingFirstSample = true;
        if (lastScreenOnElapsed < 0) lastScreenOnElapsed = SystemClock.elapsedRealtime();
        record("SENSOR_REGISTER reason=" + reason + " result=" + sensorRegistered + " name=" + light.getName());
    }

    private void stopSensor() {
        if (sensorRegistered && sensors != null) sensors.unregisterListener(this);
        if (sensorRegistered) record("SENSOR_UNREGISTER");
        sensorRegistered = false;
        waitingFirstSample = false;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!DiagnosticLog.MODE_SENSOR.equals(mode) || !sensorRegistered) return;
        float lux = event.values.length == 0 ? Float.NaN : event.values[0];
        long now = SystemClock.elapsedRealtime();
        log.latestSensor(lux, now);
        boolean first = waitingFirstSample;
        boolean changed = !Float.isFinite(lastLoggedLux) || !Float.isFinite(lux) || Math.abs(lux - lastLoggedLux) >= 1f;
        boolean periodic = lastSensorLogElapsed < 0 || now - lastSensorLogElapsed >= 10_000;
        if (first || (changed && now - lastSensorLogElapsed >= 1500) || periodic) {
            String delay = first && lastScreenOnElapsed >= 0 ? " · after screen-on=" + (now - lastScreenOnElapsed) + "ms" : "";
            record(String.format(Locale.US, "SENSOR_SAMPLE lux=%.3f%s", lux, delay));
            lastLoggedLux = lux;
            lastSensorLogElapsed = now;
        }
        waitingFirstSample = false;
        app.notifyChanged();
        refreshNotification();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        record("SENSOR_ACCURACY " + accuracy);
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        record("TASK_REMOVED · service remains requested=" + log.monitorRunning());
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) { }
        if (displays != null) displays.unregisterDisplayListener(displayListener);
        stopSensor();
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (log != null) {
            if (!intentionalStop) log.setMonitorState(false, DiagnosticLog.MODE_NONE, -1);
            record("MONITOR_SERVICE_DESTROY intentional=" + intentionalStop + " generation=" + generation);
        }
        if (thread != null) thread.quitSafely();
        if (app != null) app.notifyChanged();
        super.onDestroy();
    }

    private void record(String text) {
        if (log != null) log.append(text);
    }

    private void refreshNotification() {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,
                notification(DiagnosticLog.MODE_SENSOR.equals(mode)
                        ? "Sensor wake monitor · no keyboard writes"
                        : "Service lifecycle monitor · no keyboard writes"));
    }

    private Notification notification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "NightKeys diagnostic monitor",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Records service, screen and optional light-sensor events without changing the keyboard.");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
        PendingIntent open = PendingIntent.getActivity(this, 10, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getForegroundService(this, 11,
                new Intent(this, DiagnosticMonitorService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_keyboard_moon)
                .setContentTitle("NightKeys diagnostic lab")
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(null, "Stop monitor", stop).build())
                .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    public static void command(Context context, String action) {
        context.startForegroundService(new Intent(context, DiagnosticMonitorService.class).setAction(action));
    }
}
