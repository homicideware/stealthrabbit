package material.hunter.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.ui.activities.MainActivity;
import material.hunter.utils.NotificationUtil;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;

public class BluetoothD extends Service {

    public static final String ACTION_START = BuildConfig.APPLICATION_ID + ".services.BluetoothD.action.START";
    public static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".services.BluetoothD.action.STOP";
    private final String CHANNEL_ID = "Service";
    private BroadcastReceiver mBroadcastReceiver;
    private NotificationCompat.Builder notification;
    private Process process;
    private Timer timer = new Timer();
    private StringBuilder stderrBuilder = new StringBuilder();

    @Override
    public void onCreate() {
        mBroadcastReceiver = new BluetoothDReceiver();
        IntentFilter stopFilter = new IntentFilter(ACTION_STOP);
        stopFilter.addCategory(Intent.CATEGORY_DEFAULT);
        this.registerReceiver(mBroadcastReceiver, stopFilter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        createNotificationChannel();
        Intent notificationPendingIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationPendingIntent, NotificationUtil.setPendingIntentFlag());

        Intent stopIntent = new Intent();
        stopIntent.setAction(ACTION_STOP);
        stopIntent.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, NotificationUtil.setPendingIntentFlag());

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluebinder")
                .setContentText("Loading...")
                .setSmallIcon(R.drawable.ic_stat_mh_notification)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                .setContentIntent(pendingIntent);

        startForeground(33, notification.build());

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(33, notification.build());

        if (action != null) {
            if (action.equals(ACTION_START)) {
                new Thread(() -> {
                    try {
                        if (!isBluebinderRunning()) {
                            startBluebinder();
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    checkBluebinder();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            checkBluebinder();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, 15000);
            } else if (action.equals(ACTION_STOP)) {
                stopService();
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void startBluebinder() throws IOException, InterruptedException {
        if (process != null) {
            process.destroy();
        }

        process = Runtime.getRuntime().exec("su -mm");
        OutputStream stdin = process.getOutputStream();
        InputStream stderr = process.getErrorStream();
        InputStream stdout = process.getInputStream();
        stdin.write(
                (PathsUtil.BUSYBOX
                        + " chroot "
                        + PathsUtil.CHROOT_PATH()
                        + " "
                        + PathsUtil.CHROOT_SUDO
                        + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
                        + " su"
                        + '\n')
                        .getBytes());
        stdin.write(("bluebinder\n").getBytes());
        stdin.write(("exit\n").getBytes());
        stdin.write(("exit\n").getBytes());
        stdin.flush();
        stdin.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        String line;
        while ((line = br.readLine()) != null) {
        }
        br.close();
        br = new BufferedReader(new InputStreamReader(stderr));
        while ((line = br.readLine()) != null) {
            stderrBuilder.append(line).append("\n");
        }
        if (stderrBuilder.length() > 0)
            stderrBuilder = new StringBuilder(stderrBuilder.substring(0, stderrBuilder.length() - 1));
        br.close();
        br.close();
        process.waitFor();
    }

    private boolean isBluebinderRunning() {
        return !new ShellUtils().executeCommandAsChrootWithOutput("pidof bluebinder").isEmpty();
    }

    private void checkBluebinder() throws IOException {
        boolean isBluebinderRunning = isBluebinderRunning();
        if (isBluebinderRunning) {
            notification.setContentText("Works...");
            notification.build();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(33, notification.build());
        } else {
            notification.setContentText(
                    stderrBuilder.toString().isEmpty()
                            ? "Something is wrong, try restarting the service."
                            : stderrBuilder.toString());
            notification.build();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(33, notification.build());
        }
    }

    private void stopService() {
        unregisterReceiver(mBroadcastReceiver);
        timer.cancel();
        new ShellUtils().executeCommandAsChroot("for i in $(pidof bluebinder); do kill $i; done");
        stopForeground(true);
        stopSelf();
    }

    private class BluetoothDReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ACTION_STOP)) {
                stopService();
            }
        }
    }
}
