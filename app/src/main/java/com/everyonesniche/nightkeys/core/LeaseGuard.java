package com.everyonesniche.nightkeys.core;

/** Independent helper-process safety lease; the UI process cannot renew a dead lease. */
public final class LeaseGuard {
    public enum Verdict { KEEP, NOT_ARMED, SCREEN_OFF, CLIENT_DIED, EXPIRED, POWER_UNKNOWN }
    private final long lifetimeMs;
    private boolean armed;
    private long renewedAt;
    public LeaseGuard(long lifetimeMs) {
        if (lifetimeMs < 1000) throw new IllegalArgumentException("Lease too short");
        this.lifetimeMs = lifetimeMs;
    }
    public void arm(long now) { armed = true; renewedAt = now; }
    public boolean renew(long now, boolean interactive, boolean clientAlive) {
        if (check(now, interactive, clientAlive, true) != Verdict.KEEP) return false;
        renewedAt = now; return true;
    }
    public Verdict check(long now, boolean interactive, boolean clientAlive, boolean powerKnown) {
        if (!armed) return Verdict.NOT_ARMED;
        if (!clientAlive) return Verdict.CLIENT_DIED;
        if (!powerKnown) return Verdict.POWER_UNKNOWN;
        if (!interactive) return Verdict.SCREEN_OFF;
        if (now < renewedAt || now - renewedAt >= lifetimeMs) return Verdict.EXPIRED;
        return Verdict.KEEP;
    }
    public void disarm() { armed = false; }
    public boolean isArmed() { return armed; }
}
