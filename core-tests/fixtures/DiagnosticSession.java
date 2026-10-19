package com.everyonesniche.nightkeys.core;

/**
 * One-shot diagnostic mutations. Unlike BacklightSession, this class has no sensor,
 * screen receiver, heartbeat or automatic policy: after the requested vendor write it
 * deliberately leaves the value alone until restore() is called.
 */
public final class DiagnosticSession {
    private final BacklightSession.Port port;
    private final BacklightSession.Journal journal;

    public DiagnosticSession(BacklightSession.Port port, BacklightSession.Journal journal) {
        this.port = port;
        this.journal = journal;
    }

    public String startContinuousHold() throws Exception {
        ensureClean();
        String original = port.readTimeout();
        BacklightSession.requireTimeout(original);
        journal.save(new BacklightSession.Snapshot(original));
        port.enableContinuous();
        String observed = port.readTimeout();
        if (!BacklightSession.CONTINUOUS.equals(observed))
            throw new IllegalStateException("Continuous timeout verification failed: " + observed);
        return original;
    }

    public String startForcedOff() throws Exception {
        ensureClean();
        String original = port.readTimeout();
        BacklightSession.requireTimeout(original);
        journal.save(new BacklightSession.Snapshot(original));
        port.disableLighting();
        String observed = port.readTimeout();
        if (!"0".equals(observed))
            throw new IllegalStateException("Forced-off timeout verification failed: " + observed);
        return original;
    }

    public String restore() throws Exception {
        BacklightSession.Snapshot snapshot = journal.load();
        if (snapshot == null) return null;
        BacklightSession.requireTimeout(snapshot.timeout);
        port.restore(snapshot.timeout);
        String observed = port.readTimeout();
        if (!snapshot.timeout.equals(observed))
            throw new IllegalStateException("Restore verification failed: expected "
                    + snapshot.timeout + ", got " + observed);
        journal.clear();
        return snapshot.timeout;
    }

    private void ensureClean() throws Exception {
        if (journal.load() != null)
            throw new IllegalStateException("A hardware restore point is already pending. Restore it before starting another diagnostic write.");
    }
}
