/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.neomoddev

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelNMD
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

import java.nio.file.Files

final class NeoModDevGradleModelBuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return McpModelNMD.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        def extension = project.extensions.findByName('neoForge')
        if (extension == null) {
            return null
        }

        if (!project.plugins.findPlugin("net.neoforged.moddev")) {
            return null
        }

        def neoFormVersion
        try {
            def neoFormVersionProp = extension.neoFormVersion
            if (neoFormVersionProp instanceof String) {
                neoFormVersion = neoFormVersionProp
            } else if (neoFormVersionProp instanceof Provider) {
                neoFormVersion = neoFormVersionProp.getOrNull()
            } else {
                neoFormVersion = null
            }
        } catch (InvalidUserCodeException ignore) {
            // Happens when the NeoForm version is not set
            neoFormVersion = null
        }
        
        def neoforgeVersion
        if (neoFormVersion != null) {
            neoforgeVersion = null
        } else {
            def neoforgeVersionProp = extension.version
        
            if (neoforgeVersionProp instanceof String) {
                neoforgeVersion = neoforgeVersionProp
            } else if (neoforgeVersionProp instanceof Provider) {
                neoforgeVersion = neoforgeVersionProp.getOrNull()
            } else {
                return null
            }
        }

        def accessTransformersRaw = extension.accessTransformers
        List<File> accessTransformers
        if (accessTransformersRaw instanceof ListProperty) {
            accessTransformers = accessTransformersRaw.get().collect { project.file(it) }
        } else {
            accessTransformers = accessTransformersRaw.files.files.toList()
        }

        File mappingsFile = null
        try {
            // Hacky way to guess where the mappings file is, but I could not find a proper way to find it
            def neoformDir = project.buildDir.toPath().resolve("neoForm")
            mappingsFile = Files.list(neoformDir)
                    .map { it.resolve("config/joined.tsrg") }
                    .filter { Files.exists(it) }
                    .findFirst()
                    .orElse(null)
                    ?.toFile()
        } catch (Exception ignore) {
        }

        //noinspection GroovyAssignabilityCheck
        return new NeoModDevGradleModelImpl(neoforgeVersion, neoFormVersion, mappingsFile, accessTransformers)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev MCP project configuration")
    }
}
