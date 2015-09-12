package wav.demon.BukkitPlugin;

public class BukkitProject {

    public static final String BUKKIT_GROUP = "Bukkit Project";

    public enum Type { BUKKIT, SPIGOT, BUNGEECORD }

    private Type projectType = Type.BUKKIT;

    public Type getProjectType() {
        return projectType;
    }

    public void setProjectType(Type projectType) {
        this.projectType = projectType;
    }
}
