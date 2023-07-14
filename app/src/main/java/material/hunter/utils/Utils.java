package material.hunter.utils;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.Contract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.RegEx;

public class Utils {

    public static boolean isRoot() {
        return !new ShellUtils().executeCommandAsRootWithOutput("su -c id").isEmpty();
    }

    public static boolean isEnforcing() {
        return new ShellUtils().executeCommandAsRootWithOutput("su -c getenforce").equals("Enforcing");
    }

    public static boolean isChrootInstalled() {
        return new ShellUtils().executeCommandAsRootWithReturnCode(PathsUtil.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\"") == 0;
    }

    public static String matchString(String regex, String string, int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);
        if (matcher.find())
            if (matcher.groupCount() >= group) {
                return matcher.group(group);
            }
        return "";
    }

    public static String matchString(String regex, String string, String defaultValue, int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);
        if (matcher.find())
            if (matcher.groupCount() >= group) {
                return matcher.group(group);
            }
        return defaultValue;
    }

    @Contract(pure = true)
    public static boolean arrayContains(@NonNull String[] array, String value) {
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static boolean arrayContains(@NonNull int[] array, int value) {
        for (int item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static boolean arrayContains(@NonNull boolean[] array, boolean value) {
        for (boolean item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    public static void setErrorListener(@NonNull TextInputEditText target, TextInputLayout targetLayout, @RegEx String targetMatchPattern, String message) {
        target.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().matches(targetMatchPattern)) {
                    targetLayout.setError(message);
                } else {
                    targetLayout.setError(null);
                    targetLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for this implementation
            }
        });
    }
}
