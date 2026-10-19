package com.everyonesniche.nightkeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Persistent, bounded diagnostic event log. It never changes keyboard hardware. */
public final class DiagnosticLog {
    public static final String MODE_NONE = "none";
    public static final String MODE_LIFECYCLE = "lifecycle";
    public static final String MODE_SENSOR = "sensor";
    private static final int MAX_EVENTS = 80;
    private final SharedPreferences prefs;

    public DiagnosticLog(Context context) {
        prefs = context.getSharedPreferences("diagnostic_log", Context.MODE_PRIVATE);
    }

    public synchronized void noteAppProcessStart() {
        boolean staleMonitor = prefs.getBoolean("monitor_running", false);
        int previousPid = prefs.getInt("monitor_pid", -1);
        if (staleMonitor && previousPid != Process.myPid()) {
            appendLocked("APP_PROCESS_CREATE pid=" + Process.myPid()
                    + " · previous monitor flag remained set from pid=" + previousPid
                    + " (previous process did not record a clean monitor stop)");
        } else {
            appendLocked("APP_PROCESS_CREATE pid=" + Process.myPid());
        }
    }

    public synchronized int nextMonitorGeneration() {
        int next = prefs.getInt("monitor_generation", 0) + 1;
        if (!prefs.edit().putInt("monitor_generation", next).commit())
            throw new IllegalStateException("Could not persist diagnostic monitor generation");
        return next;
    }

    public synchronized void setMonitorState(boolean running, String mode, int pid) {
        SharedPreferences.Editor edit = prefs.edit()
                .putBoolean("monitor_running", running)
                .putString("monitor_mode", running ? mode : MODE_NONE)
                .putInt("monitor_pid", running ? pid : -1);
        if (!edit.commit()) throw new IllegalStateException("Could not save diagnostic monitor state");
    }

    public synchronized boolean monitorRunning() { return prefs.getBoolean("monitor_running", false); }
    public synchronized String monitorMode() { return prefs.getString("monitor_mode", MODE_NONE); }
    public synchronized int monitorPid() { return prefs.getInt("monitor_pid", -1); }
    public synchronized int monitorGeneration() { return prefs.getInt("monitor_generation", 0); }

    public synchronized void latestSensor(float lux, long sampleElapsedMs) {
        prefs.edit().putFloat("latest_sensor_lux", lux).putLong("latest_sensor_elapsed", sampleElapsedMs).apply();
    }

    public synchronized float latestSensorLux() { return prefs.getFloat("latest_sensor_lux", Float.NaN); }
    public synchronized long latestSensorElapsed() { return prefs.getLong("latest_sensor_elapsed", -1); }

    public synchronized void append(String event) { appendLocked(event); }

    private void appendLocked(String event) {
        String clean = event == null ? "" : event.replace('\n', ' ').replace('\r', ' ').trim();
        String line = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date())
                + "  t+" + SystemClock.elapsedRealtime() + "  " + clean;
        List<String> events = new ArrayList<>();
        String raw = prefs.getString("events", "");
        if (!raw.isEmpty()) {
            for (String existing : raw.split("\\n")) if (!existing.isBlank()) events.add(existing);
        }
        events.add(line);
        while (events.size() > MAX_EVENTS) events.remove(0);
        prefs.edit().putString("events", String.join("\n", events)).apply();
    }

    public synchronized void clear() {
        prefs.edit().remove("events").remove("latest_sensor_lux").remove("latest_sensor_elapsed").apply();
    }

    public synchronized String export() {
        String raw = prefs.getString("events", "");
        return raw.isEmpty() ? "No diagnostic events yet." : raw;
    }

    public synchronized String recent(int maxLines) {
        String raw = prefs.getString("events", "");
        if (raw.isEmpty()) return "No diagnostic events yet.";
        String[] lines = raw.split("\\n");
        int start = Math.max(0, lines.length - Math.max(1, maxLines));
        StringBuilder out = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            if (out.length() > 0) out.append('\n');
            out.append(lines[i]);
        }
        return out.toString();
    }
}
