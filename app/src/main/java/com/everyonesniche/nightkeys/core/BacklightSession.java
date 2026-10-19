package com.everyonesniche.nightkeys.core;

/**
 * Transactional orchestration for the Titan vendor timeout.
 *
 * NightKeys deliberately leaves keyboard brightness under the stock Unihertz slider.
 * During an active automation session it owns exactly one vendor key:
 * keyboard_brightness_timeout. 0 means disabled; Integer.MAX_VALUE is effectively
 * continuous. NightKeys explicitly writes 0 plus CLOSE_KEYBOARD_LIGHT on screen-off;
 * the tested firmware can relight otherwise. A durable snapshot is saved and restored
 * when automation stops.
 */
public final class BacklightSession {
    public static final String TIMEOUT = "keyboard_brightness_timeout";
    public static final String TIMEOUT_BACKUP = "keyboard_brightness_timeout_backup";
    public static final String BRIGHTNESS = "keyboard_led_brightness";
    public static final String AUTO = "keyboard_led_auto_switch";
    public static final String CONTINUOUS = "2147483647";

    public static final class Snapshot {
        public final String timeout;
        public Snapshot(String timeout) { this.timeout = timeout; }
    }

    public interface Port {
        String readTimeout() throws Exception;
        void enableContinuous() throws Exception;
        void disableLighting() throws Exception;
        boolean verifyContinuous() throws Exception;
        void restore(String originalTimeout) throws Exception;
        boolean interactive();
    }

    public interface Journal {
        Snapshot load() throws Exception;
        void save(Snapshot snapshot) throws Exception;
        void clear() throws Exception;
    }

    private final Port port;
    private final Journal journal;
    private Snapshot snapshot;
    private boolean prepared;
    private boolean lit;

    public BacklightSession(Port port, Journal journal) {
        this.port = port;
        this.journal = journal;
    }

    public void prepare() throws Exception {
        if (prepared) throw new IllegalStateException("Already prepared");
        snapshot = journal.load();
        // Heal an earlier interrupted process before acquiring a new restore point.
        if (snapshot != null) finish();
        String timeout = port.readTimeout();
        requireTimeout(timeout);
        snapshot = new Snapshot(timeout);
        journal.save(snapshot);                 // durable BEFORE first hardware mutation
        port.disableLighting();                 // fail closed until first fresh lux sample
        prepared = true;
        lit = false;
    }

    public void apply(boolean desired) throws Exception {
        if (!prepared || snapshot == null) throw new IllegalStateException("Not prepared");
        if (!desired || !port.interactive()) {
            if (lit) {
                port.disableLighting();
                lit = false;
            }
            return;
        }
        if (lit) return;
        port.enableContinuous();
        // Close the screen-off race while privileged IPC was in flight.
        if (!port.interactive()) {
            port.disableLighting();
            lit = false;
            return;
        }
        lit = true;
    }

    public void renew() throws Exception {
        if (!lit) return;
        if (!port.verifyContinuous()) {
            lit = false;
            if (port.interactive())
                throw new IllegalStateException("Vendor timeout changed outside NightKeys; automation stopped rather than fighting another control");
        }
    }

    public void finish() throws Exception {
        if (snapshot == null) snapshot = journal.load();
        if (snapshot == null) {
            prepared = false;
            lit = false;
            return;
        }
        port.restore(snapshot.timeout);
        prepared = false;
        lit = false;
        journal.clear();
        snapshot = null;
    }

    public boolean isLit() { return lit; }
    public boolean isPrepared() { return prepared; }

    public static void requireTimeout(String timeout) {
        if (timeout == null) throw new IllegalArgumentException("Vendor keyboard timeout is unavailable");
        try {
            long value = Long.parseLong(timeout);
            // -1 existed in older community mods and is accepted only as a restore value.
            if (value != -1 && (value < 0 || value > Integer.MAX_VALUE)) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unrecognized vendor timeout: " + timeout);
        }
    }
}
