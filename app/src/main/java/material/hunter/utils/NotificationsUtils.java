package material.hunter.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.os.Build;

public class NotificationsUtils {

    public static int setPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return FLAG_IMMUTABLE;
        } else {
            return FLAG_UPDATE_CURRENT;
        }
    }
}