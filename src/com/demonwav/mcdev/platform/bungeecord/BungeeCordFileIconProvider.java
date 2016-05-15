package com.demonwav.mcdev.platform.bungeecord;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BungeeCordFileIconProvider implements FileIconProvider {
    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
        if (project != null) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                BungeeCordModule bungeeCordModule = BungeeCordModule.getInstance(module);
                if (bungeeCordModule != null && file.equals(bungeeCordModule.getPluginYml())) {
                    return bungeeCordModule.getIcon();
                }
            }
        }
        return null;
    }
}
