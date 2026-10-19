package com.everyonesniche.nightkeys.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.everyonesniche.nightkeys.NightKeysApp;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        NightKeysApp app = (NightKeysApp)context.getApplicationContext();
        if (!app.journal.pending() && !(app.config.wanted() && app.config.resumeAfterBoot())) return;
        try {
            // Start safely even when Shizuku is absent. The foreground service waits without
            // sensing or lighting, and resumes/replays recovery after Shizuku is started.
            if (!app.config.resumeAfterBoot()) app.config.wanted(false);
            context.startForegroundService(new Intent(context,LightingService.class));
        } catch (Exception e) {
            app.config.error("Android blocked automatic restart. Open NightKeys: " + e.getMessage());
        }
    }
}
