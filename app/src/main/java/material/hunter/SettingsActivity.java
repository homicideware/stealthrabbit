package material.hunter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import material.hunter.utils.PathsUtil;
import material.hunter.utils.TerminalUtil;

public class SettingsActivity extends ThemedActivity {

    private static ActionBar actionBar;
    MaterialToolbar toolbar;
    private Activity activity;
    private View _view;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        activity = this;

        setContentView(R.layout.settings_activity);

        _view = getWindow().getDecorView();
        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        SwitchMaterial run_on_boot = findViewById(R.id.settings_run_on_boot);
        run_on_boot.setChecked(prefs.getBoolean("run_on_boot_enabled", true));
        run_on_boot.setOnCheckedChangeListener(
                (v, b) -> prefs.edit().putBoolean("run_on_boot_enabled", b).apply());

        SwitchMaterial show_wallpaper = findViewById(R.id.settings_show_wallpaper);
        show_wallpaper.setChecked(prefs.getBoolean("show_wallpaper", false));

        Slider background_diming_level = findViewById(R.id.settings_background_diming_level);
        background_diming_level.setEnabled(show_wallpaper.isChecked());

        int alphaLevel = prefs.getInt("background_alpha_level", 10);
        background_diming_level.setValue((float) alphaLevel / 10);
        background_diming_level.addOnSliderTouchListener(
                new Slider.OnSliderTouchListener() {
                    @Override
                    public void onStartTrackingTouch(@NonNull Slider slider) {
                    }

                    @Override
                    public void onStopTrackingTouch(@NonNull Slider slider) {
                        prefs.edit()
                                .putInt(
                                        "background_alpha_level",
                                        Double.valueOf(slider.getValue() * 10.0).intValue())
                                .apply();
                        PathsUtil.showSnack(
                                _view, "Restart recommend >", false, "Go", v -> finishAffinity());
                    }
                });

        show_wallpaper.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("show_wallpaper", b).apply();
            background_diming_level.setEnabled(b);
            PathsUtil.showSnack(
                    _view, "Restart recommend >", false, "Go", v2 -> finishAffinity());
        });

        SwitchMaterial show_timestamp = findViewById(R.id.settings_show_timestamp);
        show_timestamp.setChecked(prefs.getBoolean("print_timestamp", false));
        show_timestamp.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("print_timestamp", b).apply());

        SwitchMaterial hide_magisk_notification =
                findViewById(R.id.settings_hide_magisk_notification);
        hide_magisk_notification.setChecked(prefs.getBoolean("hide_magisk_notification", false));
        hide_magisk_notification.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean("hide_magisk_notification", b).apply());

        AutoCompleteTextView app_theme = findViewById(R.id.settings_apptheme);
        int theme = prefs.getInt("theme", 0);
        app_theme.setText(theme == 0 ? "Night" : theme == 1 ? "Day" : "Follow system", false);

        ArrayList<String> app_themes = new ArrayList<>();
        app_themes.add("Night");
        app_themes.add("Day");
        app_themes.add("Follow system");

        ArrayAdapter<String> app_themes_adapter =
                new ArrayAdapter<>(activity, R.layout.mh_spinner_item, app_themes);
        app_theme.setAdapter(app_themes_adapter);

        app_theme.setOnItemClickListener((adapterView, view, pos, l) -> {
            if (pos == 0) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (pos == 1) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            prefs.edit().putInt("theme", pos).apply();
        });

        SwitchMaterial enable_monet = findViewById(R.id.settings_enable_monet);
        enable_monet.setChecked(prefs.getBoolean("enable_monet", true));

        enable_monet.setEnabled(DynamicColors.isDynamicColorAvailable());

        enable_monet.setOnCheckedChangeListener((v, b) -> {
            prefs.edit().putBoolean("enable_monet", b).apply();
            sync();
        });

        AutoCompleteTextView select_terminal = findViewById(R.id.settings_select_terminal);
        String terminal_type = prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX);
        select_terminal.setText(terminal_type);

        ArrayList<String> terminal_types = new ArrayList<>();
        terminal_types.add(TerminalUtil.TERMINAL_TYPE_TERMUX);
        terminal_types.add(TerminalUtil.TERMINAL_TYPE_NETHUNTER);

        ArrayAdapter<String> terminal_types_adapter = new ArrayAdapter<>(activity, R.layout.mh_spinner_item, terminal_types);
        select_terminal.setAdapter(terminal_types_adapter);

        select_terminal.setOnItemClickListener((adapterView, view, pos, l) -> {
            String mTerminal_type =
                    pos == 0
                            ? TerminalUtil.TERMINAL_TYPE_TERMUX
                            : pos == 1
                            ? TerminalUtil.TERMINAL_TYPE_NETHUNTER
                            : TerminalUtil.TERMINAL_TYPE_TERMUX;
            prefs.edit().putString("terminal_type", mTerminal_type).apply();
        });
    }
}