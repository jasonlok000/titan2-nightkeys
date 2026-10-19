package com.everyonesniche.nightkeys;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.everyonesniche.nightkeys.bridge.BridgeClient;
import com.everyonesniche.nightkeys.hardware.RecoveryJournal;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NightKeysApp extends Application {
    public Config config;
    public RecoveryJournal journal;
    public DiagnosticLog diagnostics;
    public BridgeClient bridge;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    public volatile RuntimeState state = RuntimeState.idle();
    @Override public void onCreate() {
        super.onCreate();
        config = new Config(this); journal = new RecoveryJournal(this); diagnostics = new DiagnosticLog(this);
        diagnostics.noteAppProcessStart();
        bridge = new BridgeClient(this, this::notifyChanged);
    }
    public void publish(RuntimeState value) { state = value; notifyChanged(); }
    public void addListener(Runnable listener) { listeners.addIfAbsent(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    public void notifyChanged() { main.post(() -> { for (Runnable r : listeners) r.run(); }); }
}
