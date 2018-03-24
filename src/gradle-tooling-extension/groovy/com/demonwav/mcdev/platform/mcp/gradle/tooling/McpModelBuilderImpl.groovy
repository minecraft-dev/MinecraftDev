/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

final class McpModelBuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return McpModel.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        def extension = project.extensions.findByName('minecraft')
        if (extension) {
            // TODO: Add more checks to verify if ForgeGradle is actually applied in the project
            def mappingFiles = project.tasks.genSrgs.outputs.files.files.collect { it.absolutePath }
            return new McpModelImpl(extension.version, extension.mappings, mappingFiles.toSet())
        }

        // ForgeGradle is not applied so we don't need the model
        return null
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
            project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev MCP project configuration")
    }
}
