package com.everyonesniche.nightkeys.hardware;

import android.content.Context;
import android.content.SharedPreferences;
import com.everyonesniche.nightkeys.core.BacklightSession;
import org.json.JSONObject;

/** Durable restore point written synchronously before any vendor timeout change. */
public final class RecoveryJournal implements BacklightSession.Journal {
    private final SharedPreferences prefs;
    public RecoveryJournal(Context c) { prefs = c.getSharedPreferences("recovery", Context.MODE_PRIVATE); }
    public boolean pending() { return prefs.contains("snapshot"); }

    @Override public synchronized BacklightSession.Snapshot load() throws Exception {
        String raw = prefs.getString("snapshot", null);
        if (raw == null) return null;
        JSONObject j = new JSONObject(raw);
        int version = j.optInt("version", 1);
        if (version != 1 && version != 2)
            throw new IllegalStateException("Unrecognized recovery journal; do not clear app data");
        // v1 also contained master/brightness/auto. 0.9.5 no longer mutates them;
        // retaining just timeout is enough to safely recover an old interrupted record.
        return new BacklightSession.Snapshot(j.getString("timeout"));
    }

    @Override public synchronized void save(BacklightSession.Snapshot s) throws Exception {
        BacklightSession.requireTimeout(s.timeout);
        String encoded = "{\"version\":2,\"timeout\":\"" + s.timeout + "\"}";
        if (!prefs.edit().putString("snapshot", encoded).commit())
            throw new IllegalStateException("Recovery backup failed; no settings changed");
    }

    @Override public synchronized void clear() {
        if (!prefs.edit().remove("snapshot").commit())
            throw new IllegalStateException("Could not clear completed recovery record");
    }

    public synchronized String export() { return prefs.getString("snapshot", "No pending hardware changes."); }
}
