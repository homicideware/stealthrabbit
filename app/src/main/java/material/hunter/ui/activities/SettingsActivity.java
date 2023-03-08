package material.hunter.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.TerminalUtil;

public class SettingsActivity extends ThemedActivity {

    private final String TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private final String NETHUNTER_TERMINAL_PERMISSION = "com.offsec.nhterm.permission.RUN_SCRIPT_SU";
    MaterialToolbar toolbar;
    private View _view;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;
    private Slider background_diming_level;
    private TextView theme;
    private TextView terminal_emulator;
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

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        setContentView(R.layout.settings_activity);

        _view = getWindow().getDecorView();
        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SwitchMaterial run_on_boot = findViewById(R.id.settings_run_on_boot);
        run_on_boot.setChecked(prefs.getBoolean("run_on_boot_enabled", true));
        run_on_boot.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("run_on_boot_enabled", b).apply());

        SwitchMaterial show_wallpaper = findViewById(R.id.settings_show_wallpaper);
        show_wallpaper.setChecked(prefs.getBoolean("show_wallpaper", false));

        background_diming_level = findViewById(R.id.settings_background_diming_level);
        background_diming_level.setEnabled(show_wallpaper.isChecked());

        background_diming_level.setValue((float) prefs.getInt("background_alpha_level", 10) / 10);
        background_diming_level.addOnSliderTouchListener(
                new Slider.OnSliderTouchListener() {
                    @Override
                    public void onStartTrackingTouch(@NonNull Slider slider) {
                    }

                    @Override
                    public void onStopTrackingTouch(@NonNull Slider slider) {
                        prefs.edit().putInt("background_alpha_level", Double.valueOf(slider.getValue() * 10.0).intValue()).apply();
                        PathsUtil.showSnack(_view, "Restart recommend >", false, "Go", v -> hardRestartApp());
                    }
                });

        show_wallpaper.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("show_wallpaper", b).apply();
            background_diming_level.setEnabled(b);
            PathsUtil.showSnack(_view, "Restart recommend >", false, "Go", v2 -> hardRestartApp());
        });

        SwitchMaterial hide_magisk_notification = findViewById(R.id.settings_hide_magisk_notification);
        hide_magisk_notification.setChecked(prefs.getBoolean("hide_magisk_notification", false));
        hide_magisk_notification.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("hide_magisk_notification", b).apply());

        theme = findViewById(R.id.settings_theme);
        String[] themes = getResources().getStringArray(R.array.themes);
        theme.setText("Theme: " + themes[prefs.getInt("theme", 2)]);

        Button change_theme = findViewById(R.id.settings_change_theme);
        change_theme.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Change theme")
                .setItems(themes, (di, position) -> changeTheme(position, themes[position]))
                .setNegativeButton(android.R.string.cancel, (di, i) -> {
                })
                .show());

        SwitchMaterial enable_monet = findViewById(R.id.settings_enable_monet);
        enable_monet.setChecked(prefs.getBoolean("enable_monet", true));
        enable_monet.setEnabled(DynamicColors.isDynamicColorAvailable());
        enable_monet.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("enable_monet", b).apply();
            PathsUtil.showSnack(_view, "Restart recommend >", false, "Go", v2 -> hardRestartApp());
        });

        terminal_emulator = findViewById(R.id.settings_terminal_emulator);
        String[] terminal_types = getResources().getStringArray(R.array.terminal_types);
        terminal_emulator.setText("Terminal emulator: " + prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX));

        Button change_terminal_emulator = findViewById(R.id.settings_change_terminal_emulator);
        change_terminal_emulator.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
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
    }

    @SuppressLint("SetTextI18n")
    private void changeTheme(int THEME, String THEME_NAME) {
        theme.setText("Theme: " + THEME_NAME);
        prefs.edit().putInt("theme", THEME).apply();
        PathsUtil.showSnack(_view, "Restart recommend >", false, "Go", v2 -> hardRestartApp());
    }

    @SuppressLint("SetTextI18n")
    private void changeTerminal(String TERMINAL_TYPE) {
        terminal_emulator.setText("Terminal emulator: " + TERMINAL_TYPE);
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