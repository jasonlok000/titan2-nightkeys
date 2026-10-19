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
    /**
     * The Titan 2 Elite stock brightness slider is stored in a vendor-private store on
     * current firmware, not Settings.Global. NightKeys therefore does not invent or
     * overwrite a raw brightness value. The user selects the lowest visible stock
     * level once, and this records only that confirmation for the current firmware.
     */
    public void confirmStockMinimum() {
        commit(prefs.edit().putBoolean("stock_minimum_confirmed", true)
            .putString("captured_build", Build.FINGERPRINT)
            .remove("verified_build").putBoolean("test_started", false)
            .remove("minimum"));
    }
    public boolean captured() {
        if (!Build.FINGERPRINT.equals(prefs.getString("captured_build", ""))) return false;
        // Accept an older numeric calibration once, but never rely on its raw value.
        return prefs.getBoolean("stock_minimum_confirmed", false) || prefs.contains("minimum");
    }
    public boolean verified() { return captured() && Build.FINGERPRINT.equals(prefs.getString("verified_build", "")); }
    public void markVerified() {
        if (!captured()) throw new IllegalStateException("Confirm your keyboard brightness first");
        commit(prefs.edit().putString("verified_build", Build.FINGERPRINT));
    }
    public boolean wanted() { return prefs.getBoolean("wanted", false); }
    public void wanted(boolean wanted) { commit(prefs.edit().putBoolean("wanted", wanted)); }
    public void testStarted() { commit(prefs.edit().putBoolean("test_started", true)); }
    public boolean hasTest() { return prefs.getBoolean("test_started", false); }
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
