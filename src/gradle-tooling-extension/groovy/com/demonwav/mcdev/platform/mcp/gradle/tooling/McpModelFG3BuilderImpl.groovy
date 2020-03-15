/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

final class McpModelFG3BuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return McpModelFG3.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        def extension = project.extensions.findByName('minecraft')
        if (extension == null) {
            return null
        }

        if (!project.plugins.findPlugin("net.minecraftforge.gradle")) {
            return null
        }

        //  FG3 userdev
        def minecraftDepVersions = project.configurations.minecraft.dependencies.collect { it.version }

        // For some reason this task has had 2 different names...
        def task = project.tasks.findByName("createMcpToSrg")
        if (task == null) {
            task = project.tasks.findByName("createMcp2Srg")
        }
        if (task == null) {
            return null
        }

        def taskOutput = task.outputs.files.singleFile
        return new McpModelFG3Impl(minecraftDepVersions, extension.mappings, taskOutput, task.name)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev MCP project configuration")
    }
}
