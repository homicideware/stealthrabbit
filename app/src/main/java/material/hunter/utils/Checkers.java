package material.hunter.utils;

public class Checkers {

    public static boolean isRoot() {
        ShellExecuter exe = new ShellExecuter();
        return !exe.RunAsRootOutput("su -c id").isEmpty();
    }

    public static boolean isEnforcing() {
        ShellExecuter exe = new ShellExecuter();
        return exe.RunAsRootOutput("su -c getenforce").equals("Enforcing");
    }
}