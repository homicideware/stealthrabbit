package material.hunter.utils;

public class Checkers {

    public static boolean isRoot() {
        ShellUtils exe = new ShellUtils();
        return !exe.RunAsRootOutput("su -c id").isEmpty();
    }

    public static boolean isEnforcing() {
        ShellUtils exe = new ShellUtils();
        return exe.RunAsRootOutput("su -c getenforce").equals("Enforcing");
    }

    public static boolean isBusyboxInstalled() {
        return PathsUtil.getBusyboxPath() != null;
    }
}