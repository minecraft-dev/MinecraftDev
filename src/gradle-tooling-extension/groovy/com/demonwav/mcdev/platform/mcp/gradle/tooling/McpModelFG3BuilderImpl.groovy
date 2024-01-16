/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
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

        // Cheap way to be compatible with FG5
        def mappings = extension.mappings
        if (mappings instanceof Provider) {
            mappings = mappings.get()
        }

        def accessTransformers = extension.accessTransformers
        if (accessTransformers instanceof FileCollection) {
            accessTransformers = accessTransformers.asList()
        }

        //noinspection GroovyAssignabilityCheck
        return new McpModelFG3Impl(minecraftDepVersions, mappings, taskOutput, task.name, accessTransformers)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev MCP project configuration")
    }
}
