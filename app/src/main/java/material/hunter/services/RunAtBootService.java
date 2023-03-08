package material.hunter.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.Utils;

public class RunAtBootService extends Service {

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        PathsUtil.getInstance(context);
        createNotificationChannel();
    }

    private void doNotification(String contents) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, "base")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contents))
                .setContentTitle("MaterialHunter: Startup")
                .setSmallIcon(R.drawable.ic_stat_mh_notification)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(999, notification.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        if (prefs.getBoolean("run_on_boot_enabled", true)) {
            String isOK = "ok.";
            doNotification("Doing boot checks...");

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("ROOT", "access isn't granted.");
            hashMap.put("CHROOT", "isn't yet installed.");

            if (Utils.isRoot()) {
                hashMap.put("ROOT", isOK);
            }

            ShellUtils exe = new ShellUtils();

            exe.executeCommandAsRootWithOutput(PathsUtil.BUSYBOX + " run-parts " + PathsUtil.APP_INITD_PATH);
            if (exe.executeCommandAsRootWithReturnCode(PathsUtil.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\"")
                    == 0) {
                hashMap.put("CHROOT", isOK);
            }

            String resultMsg = "Boot completed.\nEverything is fine and chroot has been started.";
            for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                if (!entry.getValue().equals(isOK)) {
                    resultMsg = "Something wrong.";
                    break;
                }
            }
            doNotification(
                    "Root: "
                            + hashMap.get("ROOT")
                            + "\n"
                            + "Chroot: "
                            + hashMap.get("CHROOT")
                            + "\n"
                            + resultMsg);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel =
                    new NotificationChannel(
                            "base",
                            "Base notifications",
                            NotificationManager.IMPORTANCE_HIGH);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }
    }
}