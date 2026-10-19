package com.everyonesniche.nightkeys.service;

import android.app.Service;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.NotificationManager;
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
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Display;
import com.everyonesniche.nightkeys.Config;
import com.everyonesniche.nightkeys.NightKeysApp;
import com.everyonesniche.nightkeys.RuntimeState;
import com.everyonesniche.nightkeys.core.BacklightSession;
import com.everyonesniche.nightkeys.core.LightPolicy;
import com.everyonesniche.nightkeys.hardware.AndroidPort;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All policy/sensor/hardware work is serialized on a worker looper. The UI thread never
 * waits on vendor IPC. A separate emergency lane handles screen-off even if that worker
 * is delayed. No steady-state wake lock; the bounded lock only completes screen-off I/O.
 */
public final class LightingService extends Service implements SensorEventListener {
    public static final String START = "com.everyonesniche.nightkeys.START";
    public static final String STOP = "com.everyonesniche.nightkeys.STOP";
    public static final String TEST = "com.everyonesniche.nightkeys.TEST";
    public static final String REFRESH = "com.everyonesniche.nightkeys.REFRESH";
    private NightKeysApp app;
    private HandlerThread thread;
    private Handler worker;
    private SensorManager sensors;
    private DisplayManager displays;
    private Sensor light;
    private AndroidPort port;
    private BacklightSession session;
    private LightPolicy policy;
    private final ExecutorService emergency = Executors.newSingleThreadExecutor();
    private boolean registered, testing, stopping, screen, previousReady;
    private long testEnd, lastPublish, lastRenew;
    private String lastNotification = "";
    private boolean restoreOnly;
    private final Runnable connectionChanged = () -> {
        boolean ready = app.bridge.ready();
        if (ready == previousReady) return;
        previousReady = ready;
        if (!ready && app.journal.pending() && port != null) emergency.execute(port::emergencyOff);
        worker.post(() -> { if (!stopping) reconcile(); });
    };
    private final Runnable transition = () -> {
        if (policy != null) { policy.tick(SystemClock.elapsedRealtime()); updateOutput(true); }
    };
    private final Runnable heartbeat = () -> {
        if (stopping || session == null || !session.isPrepared()) return;
        screenChanged(false);
        if (session.isPrepared() && screen) {
            try {
                if (session.isLit()) session.renew();
                lastRenew = SystemClock.elapsedRealtime();
            } catch (Exception e) { fault(e); return; }
            scheduleHeartbeat();
        }
    };
    private final Runnable testExpiry = () -> {
        if (testing && SystemClock.elapsedRealtime() >= testEnd) stopAndRestore("Test ended · original settings restored");
    };
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            boolean off = Intent.ACTION_SCREEN_OFF.equals(intent.getAction());
            if (off && app.journal.pending()) {
                PowerManager power = getSystemService(PowerManager.class);
                PowerManager.WakeLock lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NightKeys:screenOff");
                lock.acquire(4_000);
                emergency.execute(() -> {
                    try { if (port != null) port.emergencyOff(); }
                    finally { if (lock.isHeld()) lock.release(); }
                });
            }
            worker.post(() -> screenChanged(off));
        }
    };
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) { }
        @Override public void onDisplayRemoved(int id) { if (id == Display.DEFAULT_DISPLAY) screenChanged(true); }
        @Override public void onDisplayChanged(int id) { if (id == Display.DEFAULT_DISPLAY) screenChanged(false); }
    };
    @Override public void onCreate() {
        super.onCreate(); app = (NightKeysApp)getApplication();
        startForeground(Notifications.ID,Notifications.create(this,"Starting safely…"),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        thread = new HandlerThread("nightkeys-lighting"); thread.start(); worker = new Handler(thread.getLooper());
        sensors = getSystemService(SensorManager.class); displays = getSystemService(DisplayManager.class);
        light = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        port = new AndroidPort(this,app.bridge); session = new BacklightSession(port,app.journal);
        IntentFilter filter = new IntentFilter(); filter.addAction(Intent.ACTION_SCREEN_ON); filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
        if (displays != null) displays.registerDisplayListener(displayListener,worker);
        previousReady = app.bridge.ready(); app.addListener(connectionChanged);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        worker.post(() -> {
            if (stopping) return;
            try {
                if (STOP.equals(action)) { stopAndRestore("Original keyboard settings restored"); return; }
                if (START.equals(action)) {
                    if (!app.config.verified()) throw new IllegalStateException("Finish the firmware brightness and screen-off check first");
                    testing = false; restoreOnly = false; app.config.wanted(true);
                } else if (TEST.equals(action)) {
                    if (!app.config.captured()) throw new IllegalStateException("Confirm the stock minimum brightness first");
                    if (app.config.wanted()) throw new IllegalStateException("Stop automation before running a compatibility test");
                    testing = true; restoreOnly = false; testEnd = SystemClock.elapsedRealtime() + 90_000;
                    worker.removeCallbacks(testExpiry); worker.postDelayed(testExpiry,90_000);
                } else if (REFRESH.equals(action) && session.isPrepared()) {
                    session.apply(false); rebuildPolicy(); registerSensor(false); screenChanged(false); return;
                }
                if (!testing && !app.config.wanted()) restoreOnly = true;
                reconcile();
            } catch (Exception e) { fault(e); }
        });
        return START_STICKY;
    }
    private void reconcile() {
        if (stopping) return;
        try {
            if (!app.bridge.ready()) {
                unregisterSensor(); worker.removeCallbacks(transition); worker.removeCallbacks(heartbeat);
                if (policy != null) policy.setEnabled(false);
                publish("Waiting for Shizuku", app.bridge.status(), true);
                app.bridge.connect(); return;
            }
            if (restoreOnly || (!testing && !app.config.wanted())) { stopAndRestore("Original keyboard settings restored"); return; }
            if (!Config.supportedIdentity()) throw new IllegalStateException("This device is not identified as a Titan 2 Elite in the owner profile");
            if (light == null) throw new IllegalStateException("No ambient-light sensor is available");
            if (!app.config.captured() || (!testing && !app.config.verified()))
                throw new IllegalStateException("Firmware changed or setup is incomplete. Confirm the stock brightness and verify the LEDs again");
            // If a previous process died mid-session, finish() replays the durable
            // journal before a new session is allowed to claim ownership.
            if (session.isPrepared()) session.finish();
            session.prepare();
            app.config.error("");
            if (testing) app.config.testStarted();
            rebuildPolicy(); screen = false;
            screenChanged(false);
        } catch (Exception e) { fault(e); }
    }
    private void rebuildPolicy() {
        policy = new LightPolicy(app.config.threshold(),app.config.offThreshold(),0,250);
        policy.setEnabled(true); policy.setInteractive(AndroidPort.screenOn(this));
    }
    private void screenChanged(boolean forceOff) {
        if (stopping || !session.isPrepared() || policy == null) return;
        if (testing && SystemClock.elapsedRealtime() >= testEnd) { stopAndRestore("Test ended · original settings restored"); return; }
        boolean current = !forceOff && AndroidPort.screenOn(this);
        boolean changed = current != screen;
        screen = current; policy.setInteractive(current);
        if (!current) {
            unregisterSensor(); worker.removeCallbacks(transition); worker.removeCallbacks(heartbeat);
            try { session.apply(false); } catch (Exception e) { fault(e); return; }
            publish("Screen off", "Keyboard disabled. Ambient sensing is paused.",true);
        } else {
            registerSensor(true);
            if (changed) { policy.setInteractive(false); policy.setInteractive(true); }
            updateOutput(true); scheduleHeartbeat();
        }
    }
    private void registerSensor(boolean enable) {
        if (!enable) { unregisterSensor(); return; }
        if (!registered && light != null) {
            registered = sensors.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL,0,worker);
            if (!registered) fault(new IllegalStateException("Android refused ambient-light sensor registration"));
        }
    }
    private void unregisterSensor() {
        if (registered && sensors != null) sensors.unregisterListener(this);
        registered = false;
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (stopping || !registered || !session.isPrepared() || policy == null) return;
        if (!AndroidPort.screenOn(this)) { screenChanged(true); return; }
        if (testing && SystemClock.elapsedRealtime() >= testEnd) { testExpiry.run(); return; }
        float value = event.values.length == 0 ? Float.NaN : event.values[0];
        policy.sample(value,SystemClock.elapsedRealtime()); updateOutput(false);
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Many ALS drivers report accuracy 0 constantly. Do not interpret that alone as failure.
    }
    private void updateOutput(boolean forcePublish) {
        if (stopping || policy == null || !session.isPrepared()) return;
        try {
            if (!app.bridge.ready()) { reconcile(); return; }
            boolean wasLit = session.isLit();
            session.apply(policy.shouldLight());
            if (!wasLit && session.isLit()) { lastRenew = SystemClock.elapsedRealtime(); scheduleHeartbeat(); }
            worker.removeCallbacks(transition);
            long deadline = policy.nextDeadline();
            if (deadline >= 0) worker.postDelayed(transition,Math.max(1,deadline-SystemClock.elapsedRealtime()));
            String title, detail;
            switch (policy.reason()) {
                case DARK: title = "A little light. As long as you need."; detail = "Minimum brightness requested · no typing timeout"; break;
                case SETTLING_DARK: title = "Settling into the dark"; detail = "Briefly checking the light level to avoid flicker."; break;
                case SETTLING_BRIGHT: title = "The room is getting brighter"; detail = "Keyboard lighting will switch off shortly."; break;
                case WAITING_FOR_SENSOR: title = "Reading the room"; detail = "The keyboard stays off until a fresh light reading arrives."; break;
                case INVALID_SENSOR: title = "Sensor reading unavailable"; detail = "Keyboard disabled until a valid reading arrives."; break;
                case SCREEN_OFF: title = "Screen off"; detail = "Keyboard disabled. Ambient sensing is paused."; break;
                default: title = "Bright enough without it"; detail = "Keyboard disabled · waiting for a darker room";
            }
            publish(title,detail,forcePublish || wasLit != session.isLit());
        } catch (Exception e) { fault(e); }
    }
    private void scheduleHeartbeat() {
        worker.removeCallbacks(heartbeat);
        if (screen && session.isPrepared() && !stopping) worker.postDelayed(heartbeat,30_000);
    }
    private void publish(String title, String detail, boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && now-lastPublish < 300) return;
        lastPublish = now;
        app.publish(new RuntimeState(true,app.bridge.ready() && policy != null && policy.shouldLight() && AndroidPort.screenOn(this) && session != null && session.isLit(),testing,
                policy == null ? Float.NaN : policy.lux(), title,detail,testEnd));
        String notification = testing ? "Compatibility test · " + detail : detail;
        if (!Objects.equals(lastNotification,notification)) {
            lastNotification = notification;
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                getSystemService(NotificationManager.class).notify(Notifications.ID,Notifications.create(this,notification));
        }
    }
    private void stopAndRestore(String completeMessage) {
        testing = false; restoreOnly = true;
        app.config.wanted(false);
        unregisterSensor(); worker.removeCallbacks(heartbeat); worker.removeCallbacks(transition); worker.removeCallbacks(testExpiry);
        if (policy != null) policy.setEnabled(false);
        try {
            session.finish();
            stopping = true; app.config.error("");
            app.publish(new RuntimeState(false,false,false,Float.NaN,"Automatic lighting paused",completeMessage,0));
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        } catch (Exception e) {
            if (port != null) port.emergencyOff();
            app.config.error("Restoration pending: " + clean(e));
            publish("Restoration needs attention", "Start Shizuku, then tap Stop & restore again. The recovery backup has been kept.",true);
        }
    }
    private void fault(Exception error) {
        app.config.error(clean(error));
        testing = false; restoreOnly = true;
        try { app.config.wanted(false); } catch (Exception ignored) { }
        unregisterSensor(); worker.removeCallbacks(heartbeat); worker.removeCallbacks(transition); worker.removeCallbacks(testExpiry);
        if (policy != null) policy.setEnabled(false);
        if (app.journal.pending() && port != null) port.emergencyOff();
        try {
            session.finish(); stopping = true;
            app.publish(new RuntimeState(false,false,false,Float.NaN,"Lighting paused safely",clean(error),0));
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        } catch (Exception restoreError) {
            publish("Restoration needs attention", clean(error) + " · " + clean(restoreError),true);
        }
    }
    private static String clean(Exception e) {
        String value = e.getMessage(); return value == null || value.isBlank() ? e.getClass().getSimpleName() : value;
    }
    @Override public void onDestroy() {
        app.removeListener(connectionChanged);
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) { }
        if (displays != null) displays.unregisterDisplayListener(displayListener);
        worker.removeCallbacksAndMessages(null);
        worker.post(() -> {
            unregisterSensor();
            if (!stopping) {
                try { session.finish(); } catch (Exception e) { if (port != null) port.emergencyOff(); app.config.error("Recovery pending: " + clean(e)); }
                app.publish(new RuntimeState(false,false,false,Float.NaN,"Service stopped","Reopen NightKeys to resume or restore.",0));
            }
            thread.quitSafely();
        });
        emergency.shutdown(); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    public static void command(Context context, String action) {
        context.startForegroundService(new Intent(context,LightingService.class).setAction(action));
    }
}
