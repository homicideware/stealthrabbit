package material.hunter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import material.hunter.utils.Checkers;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;

public class MainActivity extends ThemedActivity {

    private static ActionBar actionBar;
    private static MenuItem lastSelectedMenuItem;
    private static BottomNavigationView bn;
    private static boolean chroot_installed = false;
    private static float kernel_base = 1.0f;
    private static boolean busybox_installed = false;
    private static boolean selinux_enforcing = false;
    private final ShellUtils exe = new ShellUtils();
    MaterialToolbar toolbar;
    HomeFragment _home;
    ManagerFragment _manager;
    MenuFragment _menu;
    private Activity activity;
    private Context context;
    private SharedPreferences prefs;

    public static boolean addBadgeNumberForItem(int itemId) {
        if (getLastSelectedMenuItem().getItemId() != itemId) {
            BadgeDrawable badge = bn.getOrCreateBadge(itemId);
            int number = badge.getNumber();
            badge.setNumber(number + 1);
            return true;
        } else {
            return false;
        }
    }

    public static MenuItem getLastSelectedMenuItem() {
        return lastSelectedMenuItem;
    }

    public static boolean isChrootInstalled() {
        return chroot_installed;
    }

    public static void setChrootInstalled(boolean _chroot_installed) {
        chroot_installed = _chroot_installed;
        new Handler(Looper.getMainLooper()).post(() -> MenuFragment.compatVerified(_chroot_installed));
    }

    public static float getKernelBase() {
        return kernel_base;
    }

    public static void setKernelBase(float _kernel_base) {
        kernel_base = _kernel_base;
    }

    public static void setChrootStatus(String status) {
        actionBar.setSubtitle(status);
    }

    public static void setBusyboxInstalled(boolean _busybox_installed) {
        busybox_installed = _busybox_installed;
    }

    public static boolean isBusyboxInstalled() {
        return busybox_installed;
    }

    public static void setSelinuxEnforcing(boolean _selinux_enforcing) {
        selinux_enforcing = _selinux_enforcing;
    }

    public static boolean isSelinuxEnforcing() {
        return selinux_enforcing;
    }

    public static void openPage(String pageName) {
        switch (pageName) {
            case "home":
                bn.setSelectedItemId(R.id.home);
                break;
            case "manager":
                bn.setSelectedItemId(R.id.manager);
                break;
            case "menu":
                bn.setSelectedItemId(R.id.menu);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        final int theme = prefs.getInt("theme", 0);
        if (theme == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (theme == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        context = this;
        activity = this;

        // Setup utils
        PathsUtil.getInstance(context);
        busybox_installed = PathsUtil.getBusyboxPath() != null;
        selinux_enforcing = Checkers.isEnforcing();

        // Setup rootView content
        setRootView();

        if (!Checkers.isRoot()) {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(this);
            adb.setCancelable(false);
            adb.setTitle(getResources().getString(R.string.app_name));
            adb.setMessage("Root required. The app cannot work without root privileges.");
            adb.setPositiveButton(
                    "Exit",
                    (dialog, which) -> finishAffinity());
            adb.show();
        } else {
            installOrUpdate();
        }

        // Check notification post permission (A13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            String NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS;
            if (!hasPermissions(NOTIFICATION_PERMISSION)) {
                requestPermissions(NOTIFICATION_PERMISSION);
            }
        }

        // Check is termux api external apps enabled
        TerminalUtil terminal = new TerminalUtil(activity, context);
        if (prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX).equals(TerminalUtil.TERMINAL_TYPE_TERMUX)
                && terminal.isTermuxInstalled()
                && terminal.isTermuxApiSupported()) {
            terminal.termuxApiExternalAppsRequired();
        }
    }

    @Override
    protected void onDestroy() {
        bn.getOrCreateBadge(R.id.manager).clearNumber();
        super.onDestroy();
    }

    private void setRootView() {
        setContentView(R.layout.main_activity);
        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle("Home");

        bn = findViewById(R.id.bottomNavigation);

        bn.setOnItemSelectedListener(item -> changeFragment(item.getItemId(), item));

        if (lastSelectedMenuItem == null) lastSelectedMenuItem = bn.getMenu().getItem(0);

        prepareBottomNav();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (lastSelectedMenuItem == null) {
            return;
        }
        String data = intent.getDataString();
        if (data != null && !data.equals("")) {
            switch (data) {
                case "home":
                    bn.setSelectedItemId(R.id.home);
                    break;
                case "manager":
                    bn.setSelectedItemId(R.id.manager);
                    break;
                case "menu":
                    bn.setSelectedItemId(R.id.menu);
                    break;
            }
        }
    }

    protected void prepareBottomNav() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fmt = fm.beginTransaction();

        if ((_home = (HomeFragment) fm.findFragmentByTag("home")) == null)
            fmt.add(R.id.frame, (_home = new HomeFragment()), "home");

        if ((_manager = (ManagerFragment) fm.findFragmentByTag("manager")) == null)
            fmt.add(R.id.frame, (_manager = new ManagerFragment()), "manager").hide(_manager);

        if ((_menu = (MenuFragment) fm.findFragmentByTag("menu")) == null)
            fmt.add(R.id.frame, (_menu = new MenuFragment()), "menu").hide(_menu);

        fmt.commit();
    }

    private boolean changeFragment(int id, MenuItem item) {

        FragmentTransaction fmt = getSupportFragmentManager().beginTransaction();

        if (lastSelectedMenuItem != item) {
            toolbar.setAlpha(0.5f);
            toolbar.animate().alpha(1).setDuration(384);
            lastSelectedMenuItem = item;
        }

        switch (id) {
            case R.id.home:
                fmt.show(_home).hide(_manager).hide(_menu).commit();
                actionBar.setTitle("Home");
                break;
            case R.id.manager:
                fmt.show(_manager).hide(_menu).hide(_home).commit();
                actionBar.setTitle("Manager");
                bn.getOrCreateBadge(R.id.manager).clearNumber();
                bn.removeBadge(R.id.manager);
                break;
            case R.id.menu:
                fmt.show(_menu).hide(_home).hide(_manager).commit();
                actionBar.setTitle("Menu");
                break;
        }
        return true;
    }

    /* Check permissions, folders (in sdcard, data, etc..),
            copy scripts if they doesn't exciting;
            Setup default preferences
        */
    @SuppressLint("BatteryLife")
    private void installOrUpdate() {
        if (BuildConfig.VERSION_CODE == prefs.getInt("version", 0)) {
            return;
        }

        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "com.termux.permission.RUN_COMMAND",
                "com.offsec.nhterm.permission.RUN_SCRIPT_SU"
        };

        if (!hasPermissions(PERMISSIONS)) {
            requestPermissions(PERMISSIONS);
        }

        // copy (recursive) of the assets/{scripts, etc, wallpapers} folders to /data/data/...
        assetsToFiles(PathsUtil.APP_PATH, "", "data");
        exe.RunAsRoot(
                new String[]{
                        "chmod -R 700 " + PathsUtil.APP_SCRIPTS_PATH + "/*",
                        "chmod -R 700 " + PathsUtil.APP_INITD_PATH + "/*"
                });

        File mh_folder = new File(PathsUtil.APP_SD_PATH);
        if (!mh_folder.exists()) {
            try {
                mh_folder.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showMessage(
                        context,
                        "Failed to create MaterialHunter directory.",
                        false);
                return;
            }
        }

        // Setup the default SharePreference value.
        if (prefs.getString("chroot_backup_path", null) == null) {
            prefs.edit()
                    .putString("chroot_backup_path", PathsUtil.SD_PATH + "/mh-backup.tar.gz")
                    .apply();
        }
        if (prefs.getString("chroot_restore_path", null) == null) {
            prefs.edit()
                    .putString("chroot_restore_path", PathsUtil.SD_PATH + "/mh-backup.tar.gz")
                    .apply();
        }

        // Request to remove battery optimization mode and request overlay permission for Termux
        {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(String.format("package:%s", packageName)));
                context.startActivity(intent);
            }

            packageName = "com.termux";

            if (isPackageInstalled(packageName)) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent();
                    intent.setAction(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse(String.format("package:%s", packageName)));
                    context.startActivity(intent);
                }

                Intent intent = new Intent();
                intent.setAction(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                PathsUtil.showMessage(context, "Please, grant overlay permission for Termux", true);
            }

            prefs.edit().putInt("version", BuildConfig.VERSION_CODE).apply();
        }

        // Request "Manage All Files" permission for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent =
                        new Intent(
                                android.provider.Settings
                                        .ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(String.format("package:%s", context.getPackageName())));
                context.startActivity(intent);
            }
        }
    }

    private Boolean pathIsAllowed(String path, String copyType) {
        if (!path.matches("^(authors|licenses|images|sounds|webkit)")) {
            if (copyType.equals("data")) {
                if (path.equals("")) {
                    return true;
                } else if (path.startsWith("scripts")) {
                    return true;
                } else if (path.startsWith("wallpapers")) {
                    return true;
                } else return path.startsWith("etc");
            }
            return false;
        }
        return false;
    }

    private void assetsToFiles(String TARGET_BASE_PATH, String path, String copyType) {
        AssetManager assetManager = context.getAssets();
        String[] assets;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(TARGET_BASE_PATH, path);
            } else {
                String fullPath = TARGET_BASE_PATH + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists() && pathIsAllowed(path, copyType)) {
                    if (!dir.mkdirs()) {
                        exe.RunAsRoot("mkdir " + fullPath);
                    }
                }
                for (String asset : assets) {
                    String p;
                    if (path.equals("")) {
                        p = "";
                    } else {
                        p = path + "/";
                    }
                    if (pathIsAllowed(path, copyType)) {
                        assetsToFiles(TARGET_BASE_PATH, p + asset, copyType);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void copyFile(String TARGET_BASE_PATH, String filename) {
        AssetManager assetManager = context.getAssets();
        InputStream in;
        OutputStream out;
        String newFileName;
        try {
            in = assetManager.open(filename);
            newFileName = TARGET_BASE_PATH + "/" + filename;
            out = new FileOutputStream(renameAssetNeeded(newFileName));
            byte[] buffer = new byte[8092];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            exe.RunAsRoot(new String[]{"cp " + filename + " " + TARGET_BASE_PATH});
        }
    }

    // This rename the filename which suffix is either [name]-arm64 or [name]-armeabi to [name]
    // according to the user's CPU ABI.
    private String renameAssetNeeded(String asset) {
        String arch = getArch();
        if (arch != null) {
            if (asset.matches("^.*-arm64$")) {
                if (arch.equals("arm64")) {
                    return (asset.replaceAll("-arm64$", ""));
                }
            } else if (asset.matches("^.*-armeabi$")) {
                if (!arch.equals("arm64")) {
                    return (asset.replaceAll("-armeabi$", ""));
                }
            }
        }
        return asset;
    }

    private String getArch() {
        for (String androidArch : Build.SUPPORTED_ABIS) {
            switch (androidArch) {
                case "arm64-v8a":
                    return "arm64";
                case "armeabi-v7a":
                    return "arm";
                case "x86_64":
                    return "x86_64";
                case "x86":
                    return "x86";
            }
        }
        return null;
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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