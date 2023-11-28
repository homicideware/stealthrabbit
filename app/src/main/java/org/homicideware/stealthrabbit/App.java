package org.homicideware.stealthrabbit;

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
        int theme = prefs.getInt("theme", 2);
        AppCompatDelegate.setDefaultNightMode(
                theme == 0
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : theme == 1
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        boolean useDynamicColors = prefs.getBoolean("enable_monet", true);
        if (useDynamicColors) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
