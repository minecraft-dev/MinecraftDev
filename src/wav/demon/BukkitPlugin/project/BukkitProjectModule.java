package wav.demon.BukkitPlugin.project;

import wav.demon.BukkitPlugin.BukkitProject;
import wav.demon.BukkitPlugin.icons.BukkitProjectsIcons;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class BukkitProjectModule extends JavaModuleType {

    private static final String ID = "BUKKIT_MODULE_TYPE";
    private BukkitProject project = new BukkitProject();

    public BukkitProjectModule() {
        super(ID);
    }

    public static BukkitProjectModule getInstance() {
        return (BukkitProjectModule) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public ProjectBuilder createModuleBuilder() {
        return new ProjectBuilder(project);
    }

    @NotNull
    @Override
    public String getName() {
        return "Bukkit Project";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Create standard Maven Bukkit, Spigot, or BungeeCord projects";
    }

    @Override
    public Icon getBigIcon() {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProjectBig;
        else
            return BukkitProjectsIcons.SpigotProjectBig;
    }

    @Override
    public Icon getIcon() {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProject;
        else
            return BukkitProjectsIcons.SpigotProject;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProject;
        else
            return BukkitProjectsIcons.SpigotProject;
    }


}
