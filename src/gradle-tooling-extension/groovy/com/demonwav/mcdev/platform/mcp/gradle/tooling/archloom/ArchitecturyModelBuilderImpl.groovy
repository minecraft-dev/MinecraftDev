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
