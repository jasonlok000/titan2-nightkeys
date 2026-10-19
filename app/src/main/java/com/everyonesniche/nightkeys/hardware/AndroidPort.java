package com.everyonesniche.nightkeys.hardware;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.view.Display;
import com.everyonesniche.nightkeys.bridge.BridgeClient;
import com.everyonesniche.nightkeys.core.BacklightSession;

public final class AndroidPort implements BacklightSession.Port {
    private final Context context;
    private final BridgeClient bridge;

    public AndroidPort(Context context, BridgeClient bridge) {
        this.context = context;
        this.bridge = bridge;
    }

    @Override public String readTimeout() throws Exception {
        return bridge.require().inspect().getString("timeout");
    }

    @Override public void enableContinuous() throws Exception {
        bridge.require().enableContinuous();
    }

    @Override public void disableLighting() throws Exception {
        bridge.require().disableLighting();
    }

    @Override public boolean verifyContinuous() throws Exception {
        return bridge.require().verifyContinuous();
    }

    @Override public void restore(String originalTimeout) throws Exception {
        bridge.require().restore(originalTimeout);
    }

    @Override public boolean interactive() { return screenOn(context); }

    public void emergencyOff() {
        if (!bridge.ready()) return;
        try { bridge.forceOff(); } catch (Exception ignored) { }
    }

    public static boolean screenOn(Context context) {
        PowerManager power = context.getSystemService(PowerManager.class);
        DisplayManager displays = context.getSystemService(DisplayManager.class);
        Display screen = displays == null ? null : displays.getDisplay(Display.DEFAULT_DISPLAY);
        return power != null && power.isInteractive() && screen != null && screen.getState() == Display.STATE_ON;
    }
}
