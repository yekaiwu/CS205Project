package com.example.cs205;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

/**
 * A class for displaying system notifications.
 */
public class NotificationPublisher {

    public static void showNotification(Context context) {
        final String channelId = "my_notifications";
        final Object notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManager notificationManager = (NotificationManager)notificationService;
        final int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel notificationChannel = new NotificationChannel(channelId, "My notifications", importance);
        notificationChannel.setDescription("Test notifications");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.YELLOW);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{0, 250, 500, 1000});
        notificationManager.createNotificationChannel(notificationChannel);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(Notification.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setTicker("CS205")
            .setContentTitle("Game Over!")
            .setContentText("Your time is up!")
            .setContentInfo("Click to get back to the menu.");
        Notification notification = notificationBuilder.build();
        notificationManager.notify(1, notification);
    }
}
