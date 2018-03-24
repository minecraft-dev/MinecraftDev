/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle.tooling

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelBuilderImpl
import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

final class ForgePatcherModelBuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return ForgePatcherModel.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        def extension = project.extensions.findByName("minecraft")
        if (extension == null) {
            // No ForgeGradle
            return null
        }

        if (project.plugins.findPlugin("net.minecraftforge.gradle.patcher") == null) {
            // Not using the patcher plugin
            return null
        }

        def mcpModel = new McpModelBuilderImpl().buildAll(modelName, project)

        def set = new HashSet<String>()
        for (p in extension.projects) {
            set.add(p.capName)
        }

        return new ForgePatcherModelImpl(mcpModel, set)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
            project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev Forge patcher project configuration")
    }
}
