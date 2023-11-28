package org.homicideware.stealthrabbit.ui.fragments;

import android.HardwareProps;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.homicideware.stealthrabbit.BuildConfig;
import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.databinding.ManagerDialogInstallBusyboxBinding;
import org.homicideware.stealthrabbit.ui.activities.AboutActivity;
import org.homicideware.stealthrabbit.ui.activities.MainActivity;
import org.homicideware.stealthrabbit.ui.activities.SettingsActivity;
import org.homicideware.stealthrabbit.utils.PathsUtil;
import org.homicideware.stealthrabbit.utils.ShellUtils;
import org.homicideware.stealthrabbit.utils.Utils;
import org.homicideware.stealthrabbit.utils.contract.JSON;
import org.homicideware.stealthrabbit.utils.contract.Web;

public class HomeFragment extends Fragment {

    private final ShellUtils exe = new ShellUtils();
    private Activity activity;
    private Context context;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private int rotationAngle = 0;
    private boolean selinux_enforcing = true;
    private String selinux_now = "enforcing";
    private MaterialCardView sr_news_card;
    private TextView sr_news;
    private ImageView expander;
    private TextView version_installed;
    private TextView version_available;
    private ImageView upgrade;
    private MaterialCardView magisk;
    private TextView sys_info;
    private TextView material_info;
    private MaterialCardView info_card;
    private MaterialCardView selinux_card;
    private TextView selinux_status;
    private MaterialCardView telegram_card;
    private TextView telegram_title;
    private TextView telegram_description;
    private boolean primordialBusyboxBroken = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        context = getContext();
        executor = Executors.newSingleThreadExecutor();
        prefs = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkBusybox();

        sr_news_card = view.findViewById(R.id.sr_news_card);
        sr_news = view.findViewById(R.id.sr_news);
        expander = view.findViewById(R.id.expander);
        version_installed = view.findViewById(R.id.version_installed);
        version_available = view.findViewById(R.id.version_Available);
        upgrade = view.findViewById(R.id.upgrade);
        magisk = view.findViewById(R.id.magisk_card);
        sys_info = view.findViewById(R.id.sys_info);
        material_info = view.findViewById(R.id.material_info);
        info_card = view.findViewById(R.id.info_card);
        selinux_card = view.findViewById(R.id.selinux_card);
        selinux_status = view.findViewById(R.id.selinux_status);
        telegram_card = view.findViewById(R.id.telegram_card);
        telegram_title = view.findViewById(R.id.telegram_title);
        telegram_description = view.findViewById(R.id.telegram_description);

        version_installed.setText(BuildConfig.VERSION_NAME);
        sr_news.setOnTouchListener(new LinkMovementMethodOverride());

        File sr_folder = new File(PathsUtil.APP_SD_PATH);
        if (!sr_folder.exists()) {
            try {
                sr_folder.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnackBar(
                        activity,
                        "Failed to create StealthRabbit directory.",
                        false);
                return;
            }
        }

        executor.execute(() -> {
            String[] res = {""};
            try {
                res[0] = new Web().getContent("https://git.killarious.org/homicideware/stealthrabbit/raw/branch/main/repo/changelog");
                prefs.edit().putString("last_news", res[0]).apply();
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> sr_news_card.setVisibility(View.GONE));
            }
            new Handler(Looper.getMainLooper()).post(() -> sr_news.setText(res[0]));
        });

        sr_news_card.setOnClickListener(v -> {
            ObjectAnimator animation = ObjectAnimator.ofInt(sr_news, "maxLines", sr_news.getMaxLines() == 4 ? sr_news.getLineCount() : 4);
            animation.setDuration(200).start();
            rotationAngle = rotationAngle == 0 ? 180 : 0;
            expander.animate().rotation(rotationAngle).setDuration(200).start();
        });

        executor.execute(() -> {
            try {
                final JSONObject bubblegum = new JSON().getFromWeb("https://git.killarious.org/homicideware/stealthrabbit/raw/branch/main/repo/update.json");

                if (bubblegum.has("version") && bubblegum.has("code") && bubblegum.has("url")) {

                    final String new_version = bubblegum.getString("version");
                    final int code = bubblegum.getInt("code");
                    final String url = bubblegum.getString("url");

                    new Handler(Looper.getMainLooper()).post(() -> {

                        version_available.setText(new_version);

                        if (code > BuildConfig.VERSION_CODE && !new_version.equals(BuildConfig.VERSION_NAME)) {
                            upgrade.setVisibility(View.VISIBLE);
                            upgrade.setOnClickListener(v -> {
                                Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(openUrl);
                            });
                        }
                    });
                }
            } catch (JSONException | IOException ignored) {
            }
        });

        executor.execute(() -> {
            if (!magiskPassed()) {
                if (!prefs.getBoolean("hide_magisk_notification", false)) {
                    new Handler(Looper.getMainLooper()).post(() -> magisk.setVisibility(View.VISIBLE));
                }
            }
        });
        magisk.setOnClickListener(v -> executor.execute(() -> {
            if (exe.executeCommandAsRootWithReturnCode("[ -f " + PathsUtil.MAGISK_DB_PATH + " ]") == 0) {
                if (exe.executeCommandAsRootWithOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT * from policies\" | grep " + BuildConfig.APPLICATION_ID).startsWith(BuildConfig.APPLICATION_ID)) {
                    if (exe.executeCommandAsRootWithReturnCode(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE package_name='" + BuildConfig.APPLICATION_ID + "';\"") == 0) {
                        new Handler(Looper.getMainLooper()).post(() -> magisk.setVisibility(View.GONE));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Failed to hide Magisk notifications. Try to do it in Magisk app.", true));
                    }
                } else {
                    if (exe.executeCommandAsRootWithReturnCode(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE uid='$(stat -c %u /data/data/" + BuildConfig.APPLICATION_ID + ")';\"") == 0) {
                        new Handler(Looper.getMainLooper()).post(() -> magisk.setVisibility(View.GONE));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Failed to hide Magisk notifications. Try to do it in Magisk app.", true));
                    }
                }
            }
        }));
        magisk.setOnLongClickListener(v -> {
            prefs.edit().putBoolean("hide_magisk_notification", true).apply();
            magisk.setVisibility(View.GONE);
            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Warning hidden.", false);
            return true;
        });

        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();

            sb.append("Model: ").append(Build.BRAND).append(" ").append(Build.MODEL).append(" (").append(HardwareProps.getProp("ro.build.product")).append(")\n");
            sb.append("OS Version: Android ").append(Build.VERSION.RELEASE).append(", SDK ").append(Build.VERSION.SDK_INT).append("\n");
            sb.append(!TextUtils.isEmpty(PathsUtil.BUSYBOX) ? "Busybox installed in: " + PathsUtil.BUSYBOX + "\n" : "");

            String CPU = Utils.matchString("^Hardware.*: (.*)", exe.executeCommandAsRootWithOutput("cat /proc/cpuinfo | grep \"Hardware\""), 1);
            sb.append(!CPU.isEmpty() ? "CPU: " + CPU + "\n" : "");

            String kernel_version = Utils.matchString("^([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})", exe.executeCommandWithOutput("uname -r"), 1);
            sb.append("Kernel version: ").append(kernel_version).append("\n\n");

            sb.append("System-as-root: ").append(exe.executeCommandAsRootWithOutput("grep ' / ' /proc/mounts | grep -qv 'rootfs' || grep -q ' /system_root ' /proc/mounts && echo true || echo false")).append("\n");

            sb.append("Device is AB: ").append(HardwareProps.deviceIsAB() ? "true" : "false").append("\n");

            boolean usbGadgetSupported = exe.executeCommandAsRootWithReturnCode("test -d /config/usb_gadget") == 0;
            if (usbGadgetSupported) {
                sb.append("Enabled USB functions: ").append(getEnabledUSBFunctions());
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    MainActivity.setKernelBase(Float.parseFloat(Utils.matchString("^([1-9]\\.[1-9][0-9]{0,2})", kernel_version, 1)));
                } catch (NumberFormatException e) {
                    PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Failed to parse kernel base version.", false);
                }
                sys_info.setText(sb.toString());
            });
        });

        material_info.setText("Made with ❤️ by @" + BuildConfig.AUTHOR);

        info_card.setOnClickListener(v -> {
            Intent intent = new Intent(context, AboutActivity.class);
            startActivity(intent);
        });

        executor.execute(() -> {
            selinux_enforcing = Utils.isEnforcing();
            selinux_now = selinux_enforcing ? "enforcing" : "permissive";
            new Handler(Looper.getMainLooper()).post(() -> selinux_status.setText("Selinux status: " + selinux_now + ". Click to change it."));
        });
        selinux_card.setOnClickListener(v -> executor.execute(() -> {
            exe.executeCommandAsRoot("setenforce " + (selinux_enforcing ? "0" : "1"));
            selinux_enforcing = !selinux_enforcing;
            MainActivity.setSelinuxEnforcing(selinux_enforcing);
            selinux_now = selinux_enforcing ? "enforcing" : "permissive";

            new Handler(Looper.getMainLooper()).post(() -> {
                selinux_status.setText("Selinux status: " + selinux_now + ". Click to change it.");
            });
        }));

        executor.execute(() -> {
            String[] telegram_parsed = {""};
            try {
                telegram_parsed[0] = new Web().getContent("https://t.me/kali_nh");
            } catch (IOException ignored) {
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (telegram_parsed[0].isEmpty()) {
                    telegram_card.setVisibility(View.GONE);
                } else {
                    telegram_card.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/kali_nh"));
                        startActivity(intent);
                    });
                }
                telegram_title.setText(
                        Html.fromHtml(
                                Utils.matchString(".*<meta property=\"og:title\" content=\"(.*)\">", telegram_parsed[0], "N/a", 1),
                                Html.FROM_HTML_MODE_LEGACY));
                telegram_description.setText(
                        Html.fromHtml(
                                Utils.matchString(".*<meta property=\"og:description\" content=\"(.*)\">", telegram_parsed[0], 1),
                                Html.FROM_HTML_MODE_LEGACY));
                telegram_description.setSelected(true);
            });
        });
    }

    private boolean isValidBusybox(@NonNull ShellUtils.ShellObject obj) {
        if (obj.getReturnCode() == 0)
            return obj.getStdout().contains("busybox")
                    && obj.getStdout().contains("arp")
                    && obj.getStdout().contains("cat");
        return false;
    }

    private void checkBusybox() {
        executor.execute(() -> {
            String busyboxPath = prefs.getString("busybox", "");
            if (TextUtils.isEmpty(busyboxPath) && primordialBusyboxBroken) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("/data/adb/magisk/busybox");
                arrayList.add("/system/sbin/busybox");
                arrayList.add("/system/xbin/busybox");
                arrayList.add("/system/bin/busybox");
                for (String path : arrayList) {
                    if (isValidBusybox(new ShellUtils().executeCommandAsRootAndGetObject(path))) {
                        prefs.edit().putString("busybox", path).apply();
                        new Handler(Looper.getMainLooper()).post(() -> new MaterialAlertDialogBuilder(context)
                                .setTitle("Warning!")
                                .setMessage("BusyBox has been found, a restart is required for the stable operation of the application.")
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, (di, i) -> {
                                    getActivity().finishAffinity();
                                    Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    getActivity().getApplicationContext().startActivity(intent);
                                    System.exit(0);
                                })
                                .show());
                        return;
                    }
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    ManagerDialogInstallBusyboxBinding binding1 = ManagerDialogInstallBusyboxBinding.inflate(getLayoutInflater());
                    binding1.message.setText(Html.fromHtml("Busybox isn't installed, please install <a href=\"https://github.com/zgfg/BuiltIn-BusyBox\">busybox</a>. You can use <a href=\"https://github.com/Fox2Code/FoxMagiskModuleManager\">Fox's MMM</a> if you wish. If you already have basibox installed, open the app settings and configure it.", Html.FROM_HTML_MODE_LEGACY));
                    binding1.message.setMovementMethod(new LinkMovementMethod());
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Warning!")
                            .setView(binding1.getRoot())
                            .setPositiveButton(android.R.string.ok, (di, i) -> {
                            })
                            .setNegativeButton("Settings", (di, i) -> {
                                Intent intent = new Intent(getContext(), SettingsActivity.class);
                                startActivity(intent);
                            })
                            .show();
                });
            } else {
                if (!isValidBusybox(new ShellUtils().executeCommandAsRootAndGetObject(busyboxPath))) {
                    primordialBusyboxBroken = true;
                    prefs.edit().putString("busybox", "").apply();
                    new Handler(Looper.getMainLooper()).post(this::checkBusybox);
                }
            }
        });
    }

    @NonNull
    private String getEnabledUSBFunctions() {
        String result =
                exe.executeCommandAsRootWithOutput(
                        "for i in $(find /config/usb_gadget/g1/configs/b.1 -type l -exec readlink -e {} \\;); do basename $i; done | xargs");
        if (result.isEmpty()) {
            return "Nothing.";
        } else {
            return result.replace("ffs.adb", "adb");
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(context, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean magiskPassed() {
        boolean result;
        boolean res = exe.executeCommandAsRootWithOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT * from policies\" | grep " + BuildConfig.APPLICATION_ID).startsWith("(" + BuildConfig.APPLICATION_ID);
        if (res) {
            result = exe.executeCommandAsRootWithOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT notification from policies WHERE package_name='" + BuildConfig.APPLICATION_ID + "'\"").equals("0");
        } else {
            result = exe.executeCommandAsRootWithOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT notification from policies WHERE uid='$(stat -c %u /data/data/" + BuildConfig.APPLICATION_ID + ")'\"").equals("0");
        }
        return result;
    }

    // https://stackoverflow.com/a/15362634
    public static class LinkMovementMethodOverride implements View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            TextView widget = (TextView) v;
            Object text = widget.getText();
            if (text instanceof Spanned) {
                Spanned buffer = (Spanned) text;

                int action = event.getAction();

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= widget.getTotalPaddingLeft();
                    y -= widget.getTotalPaddingTop();

                    x += widget.getScrollX();
                    y += widget.getScrollY();

                    Layout layout = widget.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            link[0].onClick(widget);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
