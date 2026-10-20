package com.everyonesniche.nightkeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import java.util.Locale;

public final class Config {
    private final SharedPreferences prefs;
    public Config(Context context) { prefs = context.getSharedPreferences("configuration", Context.MODE_PRIVATE); }
    public float threshold() { return prefs.getFloat("threshold", 10f); }
    public float offThreshold() { return Math.max(threshold() + 3f, threshold() * 1.5f); }
    public void threshold(float value) {
        if (!Float.isFinite(value) || value < 0.5f || value > 100f) throw new IllegalArgumentException("Use 0.5–100 lux");
        commit(prefs.edit().putFloat("threshold", value));
    }
    public boolean wanted() { return prefs.getBoolean("wanted", false); }
    public void wanted(boolean wanted) { commit(prefs.edit().putBoolean("wanted", wanted)); }
    public boolean resumeAfterBoot() { return prefs.getBoolean("resume_after_boot", true); }
    public void resumeAfterBoot(boolean enabled) { commit(prefs.edit().putBoolean("resume_after_boot", enabled)); }
    public String lastError() { return prefs.getString("error", ""); }
    public void error(String value) { prefs.edit().putString("error", value == null ? "" : value).apply(); }
    public static boolean supportedIdentity() {
        String s = (Build.MANUFACTURER + " " + Build.MODEL + " " + Build.DISPLAY + " " + Build.FINGERPRINT).toLowerCase(Locale.ROOT);
        return Process.myUid() / 100000 == 0 && s.contains("unihertz") && s.contains("titan") && s.contains("elite");
    }
    private static void commit(SharedPreferences.Editor editor) {
        if (!editor.commit()) throw new IllegalStateException("Could not save configuration");
    }
}
