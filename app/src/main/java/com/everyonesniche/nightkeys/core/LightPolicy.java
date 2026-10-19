package com.everyonesniche.nightkeys.core;

/** Pure, single-thread-confined policy. All times are monotonic elapsed milliseconds.
 * An on-change light sensor need not emit repeatedly in a steady room: tick() completes
 * debounce without requiring a second event. Screen wake always discards the old sample.
 */
public final class LightPolicy {
    public enum Reason { DISABLED, SCREEN_OFF, WAITING_FOR_SENSOR, BRIGHT, DARK,
        SETTLING_DARK, SETTLING_BRIGHT, INVALID_SENSOR }
    private final float onLux, offLux;
    private final long onDelayMs, offDelayMs;
    private boolean enabled, interactive, lit, valid, invalidSample;
    private float lux = Float.NaN;
    private Boolean candidate;
    private long candidateSince;
    public LightPolicy(float onLux, float offLux, long onDelayMs, long offDelayMs) {
        if (!Float.isFinite(onLux) || !Float.isFinite(offLux) || onLux <= 0 || offLux <= onLux
                || onDelayMs < 0 || offDelayMs < 0) throw new IllegalArgumentException("Invalid policy");
        this.onLux = onLux; this.offLux = offLux;
        this.onDelayMs = onDelayMs; this.offDelayMs = offDelayMs;
    }
    public void setEnabled(boolean value) {
        if (enabled != value) { enabled = value; reset(); }
    }
    public void setInteractive(boolean value) {
        if (interactive != value) { interactive = value; reset(); }
    }
    private void reset() {
        lit = false; valid = false; invalidSample = false; lux = Float.NaN; candidate = null;
    }
    public void sample(float value, long now) {
        if (!enabled || !interactive) return;
        if (!Float.isFinite(value) || value < 0) {
            reset(); invalidSample = true; return;
        }
        lux = value; valid = true; invalidSample = false;
        evaluate(now);
    }
    public void tick(long now) { evaluate(now); }
    private void evaluate(long now) {
        if (!enabled || !interactive || !valid) { lit = false; candidate = null; return; }
        boolean desired = lit ? lux < offLux : lux < onLux;
        if (desired == lit) { candidate = null; return; }
        if (candidate == null || candidate != desired) { candidate = desired; candidateSince = now; }
        // A monotonic clock reset is not allowed to complete a pending transition.
        if (now < candidateSince) { candidateSince = now; return; }
        long delay = desired ? onDelayMs : offDelayMs;
        if (now - candidateSince >= delay) { lit = desired; candidate = null; }
    }
    public boolean shouldLight() { return lit && enabled && interactive && valid; }
    public float lux() { return lux; }
    public Reason reason() {
        if (!enabled) return Reason.DISABLED;
        if (!interactive) return Reason.SCREEN_OFF;
        if (invalidSample) return Reason.INVALID_SENSOR;
        if (!valid) return Reason.WAITING_FOR_SENSOR;
        if (candidate != null) return candidate ? Reason.SETTLING_DARK : Reason.SETTLING_BRIGHT;
        return lit ? Reason.DARK : Reason.BRIGHT;
    }
    public long nextDeadline() {
        return candidate == null ? -1 : candidateSince + (candidate ? onDelayMs : offDelayMs);
    }
}
