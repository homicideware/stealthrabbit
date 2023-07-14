package android;

import android.annotation.SuppressLint;

import java.lang.reflect.Method;

public class HardwareProps {

    public static String getProp(String prop) {
        String value = "";

        try {
            @SuppressLint("PrivateApi") final Class<?> sp = Class.forName("android.os.SystemProperties");
            final Method get = sp.getMethod("get", String.class);
            value = (String) get.invoke(null, prop);
        } catch (Exception ignored) {
        }
        return value;
    }

    public static boolean deviceIsAB() {
        if (getProp("ro.virtual_ab.enabled").equals("true") && getProp("ro.virtual_ab.retrofit").equals("false")) {
            return true;
        }

        /* Checks if the device supports the conventional A/B partition */
        return !getProp("ro.boot.slot_suffix").isEmpty() || getProp("ro.build.ab_update").equals("true");
    }
}
