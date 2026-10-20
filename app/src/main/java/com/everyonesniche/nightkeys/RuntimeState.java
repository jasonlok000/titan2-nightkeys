package com.everyonesniche.nightkeys;

/** Immutable UI snapshot: requested/read-back state, never measured LED output. */
public final class RuntimeState {
    public final boolean running, lightRequested;
    public final float lux;
    public final String title, detail;
    public RuntimeState(boolean running, boolean lightRequested, float lux, String title, String detail) {
        this.running = running; this.lightRequested = lightRequested;
        this.lux = lux; this.title = title; this.detail = detail;
    }
    public static RuntimeState idle() {
        return new RuntimeState(false, false, Float.NaN, "Ready when you are",
                "Install Shizuku, connect it, then enable NightKeys.");
    }
}
