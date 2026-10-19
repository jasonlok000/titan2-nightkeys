package com.everyonesniche.nightkeys;

/** Immutable UI snapshot; never reports measured LED output, only the verified requested state. */
public final class RuntimeState {
    public final boolean running, lightRequested, testing;
    public final float lux;
    public final String title, detail;
    public final long testEndMs;
    public RuntimeState(boolean running, boolean lightRequested, boolean testing, float lux,
                        String title, String detail, long testEndMs) {
        this.running = running; this.lightRequested = lightRequested; this.testing = testing;
        this.lux = lux; this.title = title; this.detail = detail; this.testEndMs = testEndMs;
    }
    public static RuntimeState idle() {
        return new RuntimeState(false, false, false, Float.NaN, "Ready when you are", "Complete setup, then enable automatic lighting.", 0);
    }
}
