package material.hunter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import material.hunter.BuildConfig;

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

    private PathsUtil(@NonNull Context context) {
        PathsUtil.context = context;
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
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
        BUSYBOX = prefs.getString("busybox", "");
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
    @NonNull
    public static String CHROOT_PATH() {
        return SYSTEM_PATH() + "/" + ARCH_FOLDER();
    }

    public static void showSnackBar(@NonNull Activity activity, String msg, boolean is_long) {
        View view = activity.findViewById(android.R.id.content);
        Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT).show();
    }

    public static void showSnackBar(@NonNull Activity activity, String msg, String actionText, View.OnClickListener listener, boolean is_long) {
        View view = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        if (!TextUtils.isEmpty(actionText) && listener != null)
            snackbar.setAction(actionText, listener);
        snackbar.show();
    }

    public static void showSnackBar(@NonNull Activity activity, View anchorView, String msg, boolean is_long) {
        View view = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        if (anchorView != null)
            snackbar.setAnchorView(anchorView);
        snackbar.show();
    }

    public static void showSnackBar(@NonNull Activity activity, View anchorView, String msg, String actionText, View.OnClickListener listener, boolean is_long) {
        View view = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, msg, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        if (anchorView != null)
            snackbar.setAnchorView(anchorView);
        if (!TextUtils.isEmpty(actionText) && listener != null)
            snackbar.setAction(actionText, listener);
        snackbar.show();
    }

    public static void showToast(Context context, String msg, boolean is_long) {
        Toast.makeText(context, msg, is_long ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}