package material.hunter.ui.activities.menu.Nmap;

public class Argument {

    private final String name;
    private final String argument;
    private final boolean enabled;

    public Argument(String name, String argument, boolean enabled) {
        this.name = name;
        this.argument = argument;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
