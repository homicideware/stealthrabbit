package material.hunter.utils;

public class CheckForRoot {

    public static boolean isRoot() {
        ShellExecuter exe = new ShellExecuter();
        return !exe.Executer("su -c 'id'").isEmpty();
    }
}

