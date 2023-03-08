package material.hunter.utils;

import android.app.Activity;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;

public class TerminalUtil {

    public static final String TERMINAL_TYPE_TERMUX = "Termux";
    public static final String TERMINAL_TYPE_NETHUNTER = "NetHunter Terminal";
    public static final String TERMUX_CHECK_EXTERNAL_APPS_IS_TRUE_CMD =
            "grep \"allow-external-apps\" /data/data/com.termux/files/home/.termux/termux.properties |"
                    + " sed -n \"s/^allow-external-apps = \\(.*\\)/\\1/p\"";
    public static final String TERMUX_SET_EXTERNAL_APPS_TRUE_CMD =
            "if [[ ! $(grep \"allow-external-apps\" /data/data/com.termux/files/home/.termux/termux.properties) ]]; then" +
                    " echo \"allow-external-apps = true\" >> /data/data/com.termux/files/home/.termux/termux.properties; else sed -i -r" +
                    " s/\"^# ?allow-external-apps = .*\"/\"allow-external-apps = true\"/g" +
                    " /data/data/com.termux/files/home/.termux/termux.properties; fi";
    private static final ShellUtils exe = new ShellUtils();
    private static ExecutorService executor;
    private static SharedPreferences prefs;
    private final Activity activity;
    private final Context context;

    private final String[] PERMISSIONS = {
            "com.termux.permission.RUN_COMMAND",
            "com.offsec.nhterm.permission.RUN_SCRIPT_SU"
    };

    public TerminalUtil(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
        executor = Executors.newSingleThreadExecutor();
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    public String getTerminalType() {
        return prefs.getString("terminal_type", TERMINAL_TYPE_TERMUX);
    }

    public void runCommand(String command, boolean in_background) throws ActivityNotFoundException, PackageManager.NameNotFoundException, SecurityException {
        String terminalType = getTerminalType();
        if (terminalType.equals(TERMINAL_TYPE_TERMUX)) {
            Intent intent = new Intent();
            intent.setClassName("com.termux", "com.termux.app.RunCommandService");
            intent.setAction("com.termux.RUN_COMMAND");
            intent.putExtra(
                    "com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/su");
            intent.putExtra(
                    "com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-mm", "-c", command});
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", in_background);
            intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    context.startService(intent);
                } catch (BackgroundServiceStartNotAllowedException e) {
                    termuxServiceIsNotRunning();
                }
            } else {
                context.startService(intent);
            }
        } else if (terminalType.equals(TERMINAL_TYPE_NETHUNTER)) {
            if (in_background) {
                executor.execute(() -> exe.executeCommandAsRoot(command));
            } else {
                Intent intent = new Intent("com.offsec.nhterm.RUN_SCRIPT_SU");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("com.offsec.nhterm.iInitialCommand", command);
                context.startActivity(intent);
            }
        }
    }

    public void showTerminalNotInstalledDialog() {
        String terminalType = getTerminalType();
        String message =
                terminalType.equals(TERMINAL_TYPE_NETHUNTER)
                        ? TERMINAL_TYPE_NETHUNTER
                        + " isn't installed, please install it from NetHunter Store."
                        : TERMINAL_TYPE_TERMUX
                        + " isn't installed, please install it from F-Droid.";
        String button =
                (terminalType.equals(TERMINAL_TYPE_NETHUNTER)
                        ? "NHStore"
                        : "F-Droid");
        String url =
                terminalType.equals(TERMINAL_TYPE_NETHUNTER)
                        ? "https://store.nethunter.com/packages/com.offsec.nhterm/"
                        : "https://f-droid.org/ru/packages/com.termux/";
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Terminal");
        adb.setMessage(message);
        adb.setPositiveButton(button, (di, i) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        });
        adb.setNegativeButton(android.R.string.ok, (di, i) -> {
        });
        adb.show();
    }

    public void showPermissionDeniedDialog() {
        String terminalType = getTerminalType();
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Terminal");
        adb.setMessage("Permission denied for starting " + terminalType + ".");
        adb.setPositiveButton(
                "Request permission",
                (di, i) -> {
                    if (!hasPermissions(PERMISSIONS)) {
                        requestPermissions(PERMISSIONS);
                    }
                });
        adb.setNegativeButton(android.R.string.ok, (di, i) -> {
        });
        adb.show();
    }

    public boolean isTermuxInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.termux", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean isNetHunterTerminalInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.offsec.nhterm", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean isTermuxApiSupported() {
        try {
            PackageInfo pi =
                    context.getPackageManager()
                            .getPackageInfo("com.termux", PackageManager.GET_SERVICES);
            for (ServiceInfo service : pi.services) {
                if (service.name.equals("com.termux.app.RunCommandService")) return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void checkIsTermuxApiSupported() {
        if (isTermuxInstalled()) {
            if (!isTermuxApiSupported()) {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                adb.setTitle("Terminal");
                adb.setMessage(
                        "Termux run command API isn't yet supported. Please install latest Termux"
                                + " version from F-Droid.");
                adb.setPositiveButton(
                        "F-Droid",
                        (di, i) -> {
                            Intent intent =
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://f-droid.org/ru/packages/com.termux/"));
                            context.startActivity(intent);
                        });
                adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
                });
                adb.show();
            }
        }
    }

    public void termuxApiExternalAppsRequired() {
        executor.execute(() -> {
            if (!exe.executeCommandAsRootWithOutput(TERMUX_CHECK_EXTERNAL_APPS_IS_TRUE_CMD).equals("true")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                    adb.setTitle("Terminal");
                    adb.setMessage(
                            "To run commands inside Termux, you need to set"
                                    + " the prop to the appropriate value so that"
                                    + " Termux allows third-party applications to"
                                    + " run commands, this is also necessary for"
                                    + " MaterialHunter.");
                    adb.setPositiveButton(
                            "Set prop",
                            (di, i) -> exe.executeCommandAsRoot(TERMUX_SET_EXTERNAL_APPS_TRUE_CMD));
                    adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
                    });
                    adb.show();
                });
            }
        });
    }

    private void termuxServiceIsNotRunning() {
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Terminal");
        adb.setMessage(
                "Termux service isn't running. Allow Termux to work in the background and run it, thus you will allow it to work in the background and wait for commands from other applications, after opening it can be immediately closed through a notification.");
        adb.setPositiveButton(
                android.R.string.ok,
                (di, i) -> {
                });
        adb.show();
    }

    private boolean hasPermissions(String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestPermissions(String... PERMISSIONS) {
        if (!hasPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 1337);
        }
    }
}