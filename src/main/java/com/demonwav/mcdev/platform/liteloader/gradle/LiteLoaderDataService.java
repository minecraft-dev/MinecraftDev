package com.demonwav.mcdev.platform.liteloader.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;

public class LiteLoaderDataService extends AbstractDataService {

    public LiteLoaderDataService() {
        super(LiteLoaderModuleType.getInstance());
    }

    @Override
    public void importData(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        checkModule(modelsProvider, LiteLoaderModuleType.getInstance(), "net.minecraftforge.gradle.liteloader");
    }
}
