package com.everyonesniche.nightkeys.core;

/** No calibration, firmware-verification flag, trial or elapsed-time gate. */
public final class Onboarding {
    private Onboarding() { }
    public static boolean canStart(boolean supported, boolean sensor, boolean connected,
                                   boolean running, boolean pendingRecovery) {
        return supported && sensor && connected && !running && !pendingRecovery;
    }
}
