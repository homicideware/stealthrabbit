package material.hunter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.DynamicColors;

import java.util.HashMap;

import mirivan.TransparentQ;

public class ThemedActivity extends AppCompatActivity {

    private static final HashMap<Activity, Resources.Theme> activities =
            new HashMap<Activity, Resources.Theme>();
    private static boolean dynamicColorsEnabled = false;
    private static final int mTargetTheme = 0;
    private SharedPreferences prefs;

    private static void setDynamicColorsEnabled(boolean b) {
        dynamicColorsEnabled = b;
    }

    public static boolean isDynamicColorsEnabled() {
        return dynamicColorsEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("material.hunter", Context.MODE_PRIVATE);
        setDynamicColorsEnabled(prefs.getBoolean("enable_monet", false));
        apply(this);
        if (prefs.getBoolean("show_wallpaper", false)) {
            getWindow()
                    .setFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            int alpha_level = prefs.getInt("background_alpha_level", 10);
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorSurface, typedValue, true);
            String color =
                    Integer.toHexString(ContextCompat.getColor(this, typedValue.resourceId))
                            .substring(2);
            getWindow()
                    .getDecorView()
                    .setBackground(
                            new ColorDrawable(
                                    Color.parseColor(TransparentQ.p2c(color, alpha_level))));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        removeActivity(this);
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private boolean apply(Activity activity) {
        if (activities.containsKey(activity)) return false;
        else {
            activities.put(activity, activity.getTheme());
            boolean useDynamicColors =
                    isDynamicColorsEnabled() && DynamicColors.isDynamicColorAvailable();
            if (useDynamicColors) {
                setTheme(R.style.ThemeM3_MaterialHunter);
                // getTheme().applyStyle(R.style.ThemeM3_MaterialHunter, false);
            } else {
                setTheme(R.style.Theme_MaterialHunter);
                // getTheme().applyStyle(R.style.Theme_MaterialHunter, false);
            }
            return true;
        }
    }

    private boolean removeActivity(Activity activity) {
        if (activities.containsKey(activity)) {
            activities.remove(activity, activity.getTheme());
            return true;
        } else return false;
    }

    public void sync() {
        activities.forEach(
                (activity, theme) -> activity.recreate());
    }
}