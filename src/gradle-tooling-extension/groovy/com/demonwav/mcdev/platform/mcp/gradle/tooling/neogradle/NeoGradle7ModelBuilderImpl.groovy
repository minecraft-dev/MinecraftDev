/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.neogradle

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelNG7
import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

final class NeoGradle7ModelBuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return McpModelNG7.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        def extension = project.extensions.findByName('minecraft')
        if (extension == null) {
            return null
        }

        if (!project.plugins.findPlugin("net.neoforged.gradle.userdev")) {
            return null
        }

        // NG userdev
        def runtimes = project.extensions.findByName('userDevRuntime').runtimes.get()
        def neoforgeVersion = null
        for (def entry in runtimes) {
            neoforgeVersion = entry.value.specification.forgeVersion
            break
        }
        if (neoforgeVersion == null) {
            return null
        }

        def mappingsFile = tasks.neoFormMergeMappings.output.get()

        def accessTransformers = extension.accessTransformers.files.asList()

        //noinspection GroovyAssignabilityCheck
        return new NeoGradle7ModelImpl(neoforgeVersion, mappingsFile, accessTransformers)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev MCP project configuration")
    }
}
