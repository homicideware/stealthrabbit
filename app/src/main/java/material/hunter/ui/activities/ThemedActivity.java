package material.hunter.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.utils.TransparentQ;

public abstract class ThemedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        if (prefs.getBoolean("show_wallpaper", false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            int alpha_level = prefs.getInt("background_alpha_level", 10);
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorSurface, typedValue, true);
            String color = Integer.toHexString(ContextCompat.getColor(this, typedValue.resourceId)).substring(2);
            getWindow().getDecorView().setBackground(new ColorDrawable(Color.parseColor(TransparentQ.p2c(color, alpha_level))));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}