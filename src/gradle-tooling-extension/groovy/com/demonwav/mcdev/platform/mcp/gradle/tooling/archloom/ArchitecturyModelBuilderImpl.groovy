/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom

import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

class ArchitecturyModelBuilderImpl implements ModelBuilderService {
    boolean canBuild(String modelName) {
        return ArchitecturyModel.name == modelName
    }

    Object buildAll(String modelName, Project project) {
        def moduleType = ArchitecturyModel.ModuleType.NONE
        if (project.getPluginManager().hasPlugin('architectury-plugin')) {
            if (project.configurations.findByName('transformProductionFabric') != null) {
                moduleType = ArchitecturyModel.ModuleType.COMMON
            } else {
                moduleType = ArchitecturyModel.ModuleType.PLATFORM_SPECIFIC
            }
        }

        return new ArchitecturyModelImpl(moduleType)
    }

    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev ArchitecturyLoom project configuration")
    }
}
