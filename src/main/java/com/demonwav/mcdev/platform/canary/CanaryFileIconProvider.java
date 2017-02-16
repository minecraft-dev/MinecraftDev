/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanaryFileIconProvider implements FileIconProvider {

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
        if (!MinecraftSettings.getInstance().isShowProjectPlatformIcons()) {
            return null;
        }

        if (project != null) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                CanaryModule canaryModule = MinecraftModule.getInstance(module, CanaryModuleType.getInstance());
                if (canaryModule == null) {
                    canaryModule = MinecraftModule.getInstance(module, NeptuneModuleType.getInstance());
                }

                if (canaryModule != null) {
                    if (file.equals(canaryModule.getCanaryInf())) {
                        return canaryModule.getIcon();
                    }
                }
            }
        }
        return null;
    }

}
