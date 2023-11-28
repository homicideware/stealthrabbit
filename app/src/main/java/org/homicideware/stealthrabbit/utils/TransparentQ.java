package org.homicideware.stealthrabbit.utils;

import androidx.annotation.NonNull;

public class TransparentQ {

    @NonNull
    public static String p2c(String defColor, int percent) {
        String alpha;
        alpha = Integer.toHexString(Math.round(255 * percent / 100));
        alpha = (alpha.length() < 2 ? "0" : "") + alpha;

        return "#" + alpha + defColor;
    }
}
