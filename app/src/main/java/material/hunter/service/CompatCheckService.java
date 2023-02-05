package material.hunter.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;

import material.hunter.MainActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;

public class CompatCheckService extends IntentService {

    private int ResultCode = -1;

    public CompatCheckService() {
        super("CompatCheckService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // if no resultCode passed by ChrootManagerFragment, then set ResultCode to -1;
        if (intent != null) {
            ResultCode = intent.getIntExtra("ResultCode", -1);
        }

        final int status = new ShellUtils().RunAsRootReturnValue(PathsUtil.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\" -p " + PathsUtil.CHROOT_PATH());

        if (ResultCode == -1) {
            if (status != 0) {
                if (status == 3) {
                    // Chroot corrupted
                } else {
                    // Remind mount
                }
                MainActivity.setChrootInstalled(false);
            } else {
                MainActivity.setChrootInstalled(true);
            }
        } else MainActivity.setChrootInstalled(ResultCode == 0);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}