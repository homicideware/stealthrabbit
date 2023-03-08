package material.hunter.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean isRoot() {
        return !new ShellUtils().executeCommandAsRootWithOutput("su -c id").isEmpty();
    }

    public static boolean isEnforcing() {
        return new ShellUtils().executeCommandAsRootWithOutput("su -c getenforce").equals("Enforcing");
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
}
