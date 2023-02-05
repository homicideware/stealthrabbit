package material.hunter.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

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
                    + " sed -n \"s/^allow-external-apps=\\(.*\\)/\\1/p\"";
    public static final String TERMUX_SET_EXTERNAL_APPS_TRUE_CMD =
            "if [[ ! $(grep \"allow-external-apps=\" /data/data/com.termux/files/home/.termux/termux.properties) ]]; then" +
                    " echo \"allow-external-apps=true\" >> /data/data/com.termux/files/home/.termux/termux.properties; else sed -i -r" +
                    " s/\"^#?allow-external-apps=.*\"/\"allow-external-apps=true\"/g" +
                    " /data/data/com.termux/files/home/.termux/termux.properties; fi";
    private static final ShellUtils exe = new ShellUtils();
    private static ExecutorService executor;
    private static SharedPreferences prefs;
    private final Activity activity;
    private final Context context;

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
            context.startService(intent);
        } else if (terminalType.equals(TERMINAL_TYPE_NETHUNTER)) {
            if (in_background) {
                executor.execute(() -> exe.RunAsRoot(command));
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
                "Open"
                        + (terminalType.equals(TERMINAL_TYPE_NETHUNTER)
                        ? "NetHunter Store"
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
        String[] PERMISSIONS = {
                "com.termux.permission.RUN_COMMAND",
                "com.offsec.nhterm.permission.RUN_SCRIPT_SU"
        };
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Terminal");
        adb.setMessage("Permission denied for starting " + terminalType + ".");
        adb.setPositiveButton(
                "Request permission",
                (di, i) -> {
                    if (!hasPermissions(PERMISSIONS)) {
                        requestTerminalPermissions();
                    }
                });
        adb.setNegativeButton(android.R.string.ok, (di, i) -> {
        });
        adb.show();
    }

    public void requestTerminalPermissions() {
        requestPermissions(
                "com.termux.permission.RUN_COMMAND",
                "com.offsec.nhterm.permission.RUN_SCRIPT_SU");
    }

    public boolean isTermuxInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.termux", 0);
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
                        "Open F-Droid",
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
            if (!exe.RunAsRootOutput(TERMUX_CHECK_EXTERNAL_APPS_IS_TRUE_CMD).equals("true")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    adb.setTitle("Termux API required external apps prop in true");
                    adb.setMessage(
                            "To run commands inside Termux, you need to set"
                                    + " the prop to the appropriate value so that"
                                    + " Termux allows third-party applications to"
                                    + " run commands, this is also necessary for"
                                    + " MaterialHunter.");
                    adb.setPositiveButton(
                            "Set prop",
                            (di, i) -> {
                                exe.RunAsRoot(TERMUX_SET_EXTERNAL_APPS_TRUE_CMD);
                            });
                    adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
                    });
                    adb.show();
                });
            }
        });
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