package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformUtil;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BukkitFileIconProvider implements FileIconProvider {
    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
        if (project != null) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                AbstractModule bukkitModule = PlatformUtil.getInstance(module);
                if (bukkitModule instanceof BukkitModule) {
                    if (file.equals(((BukkitModule) bukkitModule).getPluginYml())) {
                        return bukkitModule.getIcon();
                    }
                }
            }
        }
        return null;
    }
}
