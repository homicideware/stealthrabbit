package material.hunter.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PathsUtil {

    public static String APP_PATH;
    public static String APP_DATABASE_PATH;
    public static String APP_INITD_PATH;
    public static String APP_SCRIPTS_PATH;
    public static String APP_SCRIPTS_BIN_PATH;
    public static String SD_PATH;
    public static String APP_SD_PATH;
    public static String CHROOT_SUDO;
    public static String APP_SD_SQLBACKUP_PATH;
    public static String APP_SD_FILES_IMG_PATH;
    public static String BUSYBOX;
    public static String MAGISK_DB_PATH;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @SuppressLint("StaticFieldLeak")
    private static PathsUtil instance;
    private static SharedPreferences prefs;

    private PathsUtil(Context context) {
        PathsUtil.context = context;
        prefs = context.getSharedPreferences("material.hunter", Context.MODE_PRIVATE);
        APP_PATH = context.getFilesDir().getPath();
        APP_DATABASE_PATH = APP_PATH.replace("/files", "/databases");
        APP_INITD_PATH = APP_PATH + "/etc/init.d";
        APP_SCRIPTS_PATH = APP_PATH + "/scripts";
        APP_SCRIPTS_BIN_PATH = APP_SCRIPTS_PATH + "/bin";
        SD_PATH = Environment.getExternalStorageDirectory().toString();
        APP_SD_PATH = SD_PATH + "/MaterialHunter";
        APP_SD_SQLBACKUP_PATH = APP_SD_PATH + "/Databases";
        APP_SD_FILES_IMG_PATH = APP_SD_PATH + "/Images";
        CHROOT_SUDO = "/usr/bin/sudo";
        BUSYBOX = getBusyboxPath() != null ? getBusyboxPath().getValue() : "";
        MAGISK_DB_PATH = "/data/adb/magisk.db";
    }

    public static synchronized PathsUtil getInstance(Context context) {
        if (instance == null) {
            instance = new PathsUtil(context);
        }
        return instance;
    }

    // Directory with chroots
    public static String SYSTEM_PATH() {
        return prefs.getString("chroot_system_path", "/data/local/nhsystem");
    }

    // Chroot directory name
    public static String ARCH_FOLDER() {
        return prefs.getString("chroot_directory", "chroot");
    }

    // Full path to chroot directory
    public static String CHROOT_PATH() {
        return SYSTEM_PATH() + "/" + ARCH_FOLDER();
    }

    public static Map.Entry<String, String> getBusyboxPath() {
        HashMap<String, String> busybox = new HashMap<>();
        busybox.put("Magisk", "/data/adb/magisk/busybox");
        busybox.put("xbin", "/system/xbin/busybox");
        busybox.put("bin", "/system/bin/busybox");
        for (Map.Entry<String, String> entry : busybox.entrySet()) {
            File file = new File(entry.getValue());
            if (file.exists()) {
                return entry;
            }
        }
        return null;
    }

    public static float dpToPx(float dp) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static int getNavigationBarHeight() {
        Resources resources = context.getResources();
        @SuppressLint({"InternalInsetResource", "DiscouragedApi"}) int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    public static void showMessage(Context context, String msg, boolean is_long) {
        if (is_long) Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showSnack(View view, String msg, boolean is_long) {
        Snackbar snackbar = Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        View mView = snackbar.getView();
        mView.setTranslationY(dpToPx(-(getNavigationBarHeight())));
        snackbar.show();
    }

    public static void showSnack(View view, String msg, boolean is_long, String actionText, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        if (actionText != null && listener != null) {
            snackbar.setAction(actionText, listener);
        }
        View mView = snackbar.getView();
        mView.setTranslationY(dpToPx(-(getNavigationBarHeight())));
        snackbar.show();
    }
}