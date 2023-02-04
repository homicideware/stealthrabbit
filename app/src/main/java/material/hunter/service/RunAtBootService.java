package material.hunter.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

import material.hunter.R;
import material.hunter.utils.Checkers;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellExecuter;

public class RunAtBootService extends JobIntentService {

    private Context context;
    private NotificationCompat.Builder notification;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, RunAtBootService.class, 1, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        PathsUtil.getInstance(context);
        createNotificationChannel();
    }

    private void doNotification(String contents) {
        notification = new NotificationCompat.Builder(context, "base")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contents))
                .setContentTitle("MaterialHunter: Startup")
                .setSmallIcon(R.drawable.ic_stat_ic_nh_notificaiton)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(999, notification.build());
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        onHandleIntent();
    }

    protected void onHandleIntent() {
        SharedPreferences prefs = getSharedPreferences("material.hunter", Context.MODE_PRIVATE);
        if (prefs.getBoolean("run_on_boot_enabled", true)) {
            // 1. Check root and chroot status -> 2. Run materialhunter init.d files. -> 3. Push
            // notifications.
            String isOK = "ok.";
            doNotification("Doing boot checks...");

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("ROOT", "access isn't granted.");
            hashMap.put("CHROOT", "isn't yet installed.");

            if (Checkers.isRoot()) {
                hashMap.put("ROOT", isOK);
            }

            ShellExecuter exe = new ShellExecuter();

            exe.RunAsRootOutput(PathsUtil.BUSYBOX + " run-parts " + PathsUtil.APP_INITD_PATH);
            if (exe.RunAsRootReturnValue(PathsUtil.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\"")
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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