package com.demonwav.mcdev.platform.bukkit;

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
                BukkitModule bukkitModule = BukkitModule.getInstance(module);
                if (bukkitModule != null) {
                    if (file.equals(bukkitModule.getPluginYml())) {
                        return bukkitModule.getIcon();
                    }
                }
            }
        }
        return null;
    }
}
