package com.everyonesniche.nightkeys.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.everyonesniche.nightkeys.R;
import com.everyonesniche.nightkeys.ui.MainActivity;

final class Notifications {
    static final String CHANNEL = "automatic_keyboard_lighting";
    static final int ID = 104;
    static Notification create(Context context, String text) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL,"Keyboard lighting",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Visible status and a stop-and-restore control for automatic keyboard lighting.");
        channel.setShowBadge(false); manager.createNotificationChannel(channel);
        PendingIntent open = PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getForegroundService(context,1,
                new Intent(context,LightingService.class).setAction(LightingService.STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(context,CHANNEL)
                .setSmallIcon(R.drawable.ic_keyboard_moon).setContentTitle("NightKeys")
                .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .setShowWhen(false).setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(null,"Stop & restore",stop).build()).build();
    }
}
