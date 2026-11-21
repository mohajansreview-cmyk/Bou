package com.akshaykadam.pixelboard.extension.updater;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity;

/**
 * Checks Bou's GitHub releases at most once a day while the keyboard is in use, and posts one
 * notification per new version. Tapping it opens the Bou updates screen, where one tap installs.
 * Android never lets an app outside the Play Store install itself silently.
 */
public final class BouUpdateNotifier {

    private static final String TAG = "BouUpdate";
    private static final String PREFS = "bou_update_notifier";
    private static final String KEY_LAST_CHECK = "last_check_ms";
    private static final String KEY_NOTIFIED_VERSION = "notified_version";
    private static final String CHANNEL_ID = "bou_updates";
    private static final int NOTIFICATION_ID = 0x0B0B;
    static final long CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000;
    /** Cheap in-memory throttle: the keyboard calls in often, prefs are read at most once a minute. */
    private static final long CALL_THROTTLE_MS = 60_000;

    private static volatile long lastCallMs;

    private BouUpdateNotifier() {
    }

    /** Safe to call on every keyboard start; does network work on the updater's own thread. */
    public static void maybeCheck(Context context) {
        try {
            long now = System.currentTimeMillis();
            if (context == null || now - lastCallMs < CALL_THROTTLE_MS) {
                return;
            }
            lastCallMs = now;
            if (!PixelBoardUpdateManager.isAutoCheckEnabled(context)) {
                return;
            }
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!isDue(prefs.getLong(KEY_LAST_CHECK, 0L), now)) {
                return;
            }
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply();
            final Context app = context.getApplicationContext();
            String installed = PixelBoardUpdateFeature.resolveInstalledVersion(app);
            PixelBoardUpdateManager.getInstance().checkForUpdates(app, installed, (status, info, message) -> {
                if (status != PixelBoardUpdateManager.Status.UPDATE_AVAILABLE || info == null) {
                    return;
                }
                String version = info.getVersion();
                if (version == null || version.equals(prefs.getString(KEY_NOTIFIED_VERSION, ""))) {
                    return;
                }
                if (post(app, version)) {
                    prefs.edit().putString(KEY_NOTIFIED_VERSION, version).apply();
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Update check skipped", t);
        }
    }

    static boolean isDue(long lastCheckMs, long nowMs) {
        return lastCheckMs <= 0 || nowMs - lastCheckMs >= CHECK_INTERVAL_MS || nowMs < lastCheckMs;
    }

    private static boolean post(Context context, String version) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return false; // asked for in Bou settings; try again with the next version
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Bou updates", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, GboardPatchesSettingsActivity.class)
                .setAction(GboardPatchesSettingsActivity.ACTION_UPDATES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification n = builder
                .setSmallIcon(context.getApplicationInfo().icon)
                .setContentTitle("Bou-এর নতুন version এসেছে")
                .setContentText("Bou " + version + " পেতে চাপুন। আগের সব সেটিং থাকবে।")
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build();
        nm.notify(NOTIFICATION_ID, n);
        return true;
    }
}
