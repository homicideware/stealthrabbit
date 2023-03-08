package material.hunter;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        AppCompatDelegate.setDefaultNightMode(
                theme == 2
                        ? theme - 3
                        : theme
        );
        boolean useDynamicColors = prefs.getBoolean("enable_monet", true);
        if (useDynamicColors) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
