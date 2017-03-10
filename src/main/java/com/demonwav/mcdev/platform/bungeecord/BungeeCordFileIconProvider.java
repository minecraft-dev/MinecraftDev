/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.facet.MinecraftFacet;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BungeeCordFileIconProvider implements FileIconProvider {

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
        if (!MinecraftSettings.getInstance().isShowProjectPlatformIcons()) {
            return null;
        }

        if (project == null) {
            return null;
        }

        final Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) {
            return null;
        }

        final BungeeCordModule bungeecordModule = MinecraftFacet.getInstance(module, BungeeCordModuleType.INSTANCE);
        if (bungeecordModule == null) {
            return null;
        }

        if (file.equals(bungeecordModule.getPluginYml())) {
            return bungeecordModule.getIcon();
        }
        return null;
    }
}
