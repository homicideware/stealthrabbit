package material.hunter.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.InputDialogBinding;
import material.hunter.databinding.SettingsActivityBinding;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;

public class SettingsActivity extends ThemedActivity {

    private final String TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private final String NETHUNTER_TERMINAL_PERMISSION = "com.offsec.nhterm.permission.RUN_SCRIPT_SU";
    private SettingsActivityBinding binding;
    private SharedPreferences prefs;
    private final ActivityResultLauncher<String> requestNetHunterTerminalPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result) {
                    changeTerminal(TerminalUtil.TERMINAL_TYPE_NETHUNTER);
                } else {
                    changeTerminal(TerminalUtil.TERMINAL_TYPE_TERMUX);
                }
            }
    );
    private TerminalUtil terminalUtil;
    private final ActivityResultLauncher<String> requestTermuxPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result) {
                    terminalUtil.termuxApiExternalAppsRequired();
                    changeTerminal(TerminalUtil.TERMINAL_TYPE_TERMUX);
                } else {
                    changeTerminal(TerminalUtil.TERMINAL_TYPE_NETHUNTER);
                }
            }
    );
    private ExecutorService executor;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PathsUtil.getInstance(this);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);
        executor = Executors.newSingleThreadExecutor();

        setContentView(binding.getRoot());

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.runOnBoot.setOnClickListener(v -> binding.runOnBootSwitch.toggle());
        binding.runOnBootSwitch.setChecked(prefs.getBoolean("run_on_boot_enabled", true));
        binding.runOnBootSwitch.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("run_on_boot_enabled", b).apply());

        binding.showWallpaper.setOnClickListener(v -> binding.showWallpaperSwitch.toggle());
        binding.showWallpaperSwitch.setChecked(prefs.getBoolean("show_wallpaper", false));
        binding.showWallpaperSwitch.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("show_wallpaper", b).apply();
            binding.backgroundBlackoutLevel.setEnabled(b);
        });

        binding.backgroundBlackoutLevel.setEnabled(binding.showWallpaperSwitch.isChecked());
        binding.backgroundBlackoutLevel.setValue((float) prefs.getInt("background_blackout_level", 10) / 10);
        binding.backgroundBlackoutLevel.addOnSliderTouchListener(
                new Slider.OnSliderTouchListener() {
                    @Override
                    public void onStartTrackingTouch(@NonNull Slider slider) {
                    }

                    @Override
                    public void onStopTrackingTouch(@NonNull Slider slider) {
                        prefs.edit().putInt("background_blackout_level", Double.valueOf(slider.getValue() * 10.0).intValue()).apply();
                    }
                });

        binding.hideMagiskNotification.setOnClickListener(v -> binding.hideMagiskNotificationSwitch.toggle());
        binding.hideMagiskNotificationSwitch.setChecked(prefs.getBoolean("hide_magisk_notification", false));
        binding.hideMagiskNotificationSwitch.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("hide_magisk_notification", b).apply());

        String[] themes = getResources().getStringArray(R.array.themes);
        binding.theme.setText("Theme: " + themes[prefs.getInt("theme", 2)]);

        binding.changeTheme.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Change theme")
                .setItems(themes, (di, position) -> changeTheme(position, themes[position]))
                .setNegativeButton(android.R.string.cancel, (di, i) -> {
                })
                .show());

        binding.enableMonet.setOnClickListener(v -> binding.enableMonetSwitch.toggle());
        binding.enableMonetSwitch.setChecked(prefs.getBoolean("enable_monet", true));
        binding.enableMonetSwitch.setEnabled(DynamicColors.isDynamicColorAvailable());
        binding.enableMonetSwitch.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("enable_monet", b).apply();
        });

        String[] terminal_types = getResources().getStringArray(R.array.terminal_types);
        binding.terminalEmulator.setText("Terminal emulator: " + prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX));

        binding.changeTerminalEmulator.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Change Terminal emulator")
                .setItems(terminal_types, (di, position) -> {
                    String NEW_TERMINAL_TYPE = terminal_types[position];
                    changeTerminal(NEW_TERMINAL_TYPE);
                    if (NEW_TERMINAL_TYPE.equals(TerminalUtil.TERMINAL_TYPE_TERMUX)) {
                        if (terminalUtil.isTermuxInstalled()) {
                            if (!terminalUtil.isTermuxApiSupported()) {
                                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(this);
                                adb.setTitle("Terminal");
                                adb.setMessage(
                                        "Termux run command API isn't yet supported. Please install latest Termux"
                                                + " version from F-Droid.");
                                adb.setPositiveButton(
                                        "F-Droid",
                                        (di2, i) -> {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/ru/packages/com.termux/"));
                                            startActivity(intent);
                                        });
                                adb.setNegativeButton(android.R.string.cancel, (di2, i) -> {
                                });
                                adb.show();
                            } else {
                                requestTermuxPermissionLauncher.launch(TERMUX_PERMISSION);
                            }
                        } else {
                            terminalUtil.showTerminalNotInstalledDialog();
                            changeTerminal(TerminalUtil.TERMINAL_TYPE_NETHUNTER);
                        }
                    } else if (NEW_TERMINAL_TYPE.equals(TerminalUtil.TERMINAL_TYPE_NETHUNTER)) {
                        if (terminalUtil.isNetHunterTerminalInstalled()) {
                            requestNetHunterTerminalPermissionLauncher.launch(NETHUNTER_TERMINAL_PERMISSION);
                        } else {
                            terminalUtil.showTerminalNotInstalledDialog();
                            changeTerminal(TerminalUtil.TERMINAL_TYPE_TERMUX);
                        }
                    } else {
                        changeTerminal(TerminalUtil.TERMINAL_TYPE_TERMUX);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (di, i) -> {
                })
                .show());

        prefs.registerOnSharedPreferenceChangeListener((prefs, key) -> {
            switch (key) {
                case "show_wallpaper":
                case "background_blackout_level":
                case "enable_monet":
                case "theme":
                case "busybox":
                    PathsUtil.showSnackBar(this, "Restart recommend >", "Go", v -> hardRestartApp(), false);
                    break;
            }
        });

        String BUSYBOX = prefs.getString("busybox", "");
        binding.busybox.setText(
                "Busybox" +
                        (TextUtils.isEmpty(BUSYBOX)
                                ? " - isn't yet installed."
                                : " installed in: " + BUSYBOX)
        );

        binding.changeBusybox.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Change busybox")
                    .setMessage("BusyBox is a software suite that combines several Unix utilities into a single executable file. It is commonly used on embedded systems and in Linux distributions for basic system management tasks. It is required for most MaterialHunter components to work.")
                    .setNeutralButton(android.R.string.cancel, (di, i) -> {
                    })
                    .setNegativeButton("Select", (di, i) -> executor.execute(() -> {
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add("/data/adb/magisk/busybox");
                        arrayList.add("/system/xbin/busybox");
                        arrayList.add("/system/bin/busybox");
                        ArrayList<String> newArrayList = new ArrayList<>();
                        boolean busyboxIsInstalled = false;
                        for (String path : arrayList) {
                            if (isValidBusybox(new ShellUtils().executeCommandAsRootAndGetObject(path))) {
                                busyboxIsInstalled = true;
                                newArrayList.add(path);
                            }
                        }

                        if (busyboxIsInstalled) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                di.cancel();
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle("Select busybox")
                                        .setItems(
                                                newArrayList.toArray(new String[newArrayList.size()]),
                                                (di1, i1) -> changeBusybox(newArrayList.get(i1)))
                                        .setNegativeButton(android.R.string.cancel, (di2, i2) -> {
                                        })
                                        .show();
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showToast(
                                    this, "Busybox - isn't yet installed.", false));
                        }
                    }))
                    .setPositiveButton("Custom", (di, i) -> {
                        InputDialogBinding binding1 = InputDialogBinding.inflate(getLayoutInflater());
                        binding1.editText.setHint("Enter path to busybox:");
                        binding1.editText.setText(prefs.getString("busybox", ""));
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Custom busybox")
                                .setMessage("Enter the path to the busybox binary, it will be used by the application.")
                                .setPositiveButton(android.R.string.ok, (di1, i1) -> {
                                    String editTextString = binding1.editText.getText().toString();
                                    if (editTextString.matches("^[A-z0-9./-_]+$")) {
                                        executor.execute(() -> {
                                            if (isValidBusybox(new ShellUtils().executeCommandAsRootAndGetObject(editTextString))) {
                                                new Handler(Looper.getMainLooper()).post(() -> changeBusybox(editTextString));
                                            } else new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showToast(
                                                    this, "Invalid busybox.", false));
                                        });
                                    } else {
                                        PathsUtil.showToast(this, "Invalid path.", false);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, (di1, i1) -> {})
                                .setView(binding1.getRoot())
                                .show();
                    })
                    .show();
        });

    }

    private boolean isValidBusybox(@NonNull ShellUtils.ShellObject obj) {
        if (obj.getReturnCode() == 0)
            return obj.getStdout().contains("busybox")
                    && obj.getStdout().contains("arp")
                    && obj.getStdout().contains("cat");
        return false;
    }

    @SuppressLint("SetTextI18n")
    private void changeBusybox(String BUSYBOX) {
        binding.busybox.setText(
                "Busybox" +
                        (TextUtils.isEmpty(BUSYBOX)
                            ? " - isn't yet installed."
                            : " installed in: " + BUSYBOX)
        );
        prefs.edit().putString("busybox", BUSYBOX).apply();
    }

    @SuppressLint("SetTextI18n")
    private void changeTheme(int THEME, String THEME_NAME) {
        binding.theme.setText("Theme: " + THEME_NAME);
        prefs.edit().putInt("theme", THEME).apply();
    }

    @SuppressLint("SetTextI18n")
    private void changeTerminal(String TERMINAL_TYPE) {
        binding.terminalEmulator.setText("Terminal emulator: " + TERMINAL_TYPE);
        prefs.edit().putString("terminal_type", TERMINAL_TYPE).apply();
    }

    private void hardRestartApp() {
        finishAffinity();
        Intent intent = getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("settings"));
        getApplicationContext().startActivity(intent);
        System.exit(0);
    }
}