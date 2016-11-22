/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ForgeFileIconProvider implements FileIconProvider {

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
        if (!MinecraftSettings.getInstance().isShowProjectPlatformIcons()) {
            return null;
        }

        if (project != null) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                final ForgeModule forgeModule = MinecraftModule.getInstance(module, ForgeModuleType.getInstance());
                if (forgeModule != null) {
                    if (file.equals(forgeModule.getMcmod())) {
                        return forgeModule.getIcon();
                    }
                }
            }
        }
        return null;
    }
}
