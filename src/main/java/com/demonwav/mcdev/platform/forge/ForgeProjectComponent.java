/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.demonwav.mcdev.util.AbstractProjectComponent;
import com.demonwav.mcdev.util.Util;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ForgeProjectComponent extends AbstractProjectComponent {

    protected ForgeProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        // assign mcmod.info json thing
        Util.runWriteTaskLater(() -> {
            FileTypeManager.getInstance().associate(JsonFileType.INSTANCE, new FileNameMatcher() {
                @Override
                public boolean accept(@NonNls @NotNull String fileName) {
                    return fileName.equals(ForgeConstants.MCMOD_INFO);
                }

                @NotNull
                @Override
                public String getPresentableString() {
                    return ForgeConstants.MCMOD_INFO;
                }
            });
        });
    }
}
