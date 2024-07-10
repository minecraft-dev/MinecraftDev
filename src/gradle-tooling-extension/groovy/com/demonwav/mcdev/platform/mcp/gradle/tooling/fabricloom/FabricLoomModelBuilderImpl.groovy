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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom


import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

class FabricLoomModelBuilderImpl implements ModelBuilderService {

    @Override
    boolean canBuild(String modelName) {
        return FabricLoomModel.name == modelName
    }

    @Override
    Object buildAll(String modelName, Project project) {
        if (!project.plugins.hasPlugin('fabric-loom')) {
            return null
        }

        def loomExtension = project.extensions.getByName('loom')

        try {
            return build(project, loomExtension)
        } catch (GroovyRuntimeException ignored) {
            // Must be using an older loom version, fallback.
            return buildLegacy(project, loomExtension)
        }
    }

    FabricLoomModel build(Project project, Object loomExtension) {
        def minecraftVersion = loomExtension.minecraftProvider.minecraftVersion()
        def tinyMappings = loomExtension.mappingsFile
        def splitMinecraftJar = loomExtension.areEnvironmentSourceSetsSplit()

        def decompilers = [:]

        if (splitMinecraftJar) {
            decompilers << ["common": getDecompilers(loomExtension, false)]
            decompilers << ["client": getDecompilers(loomExtension, true)]
        } else {
            decompilers << ["single": getDecompilers(loomExtension, false)]
        }

        def modSourceSets = [:]

        for (def mod in loomExtension.getMods()) {
            def modName = mod.getName()
            modSourceSets[modName] = mod.getModSourceSets().getOrNull()?.collect { it.sourceSet().getName() }
        }

        //noinspection GroovyAssignabilityCheck
        return new FabricLoomModelImpl(minecraftVersion, tinyMappings, decompilers, splitMinecraftJar, modSourceSets)
    }

    List<FabricLoomModelImpl.DecompilerModelImpl> getDecompilers(Object loomExtension, boolean client) {
        loomExtension.decompilerOptions.collect {
            def task = loomExtension.getDecompileTask(it, client)
            def sourcesPath = task.outputJar.get().getAsFile().getAbsolutePath()
            new FabricLoomModelImpl.DecompilerModelImpl(name: it.name, taskName: task.name, sourcesPath: sourcesPath)
        }
    }

    FabricLoomModel buildLegacy(Project project, Object loomExtension) {
        def tinyMappings = loomExtension.mappingsProvider.tinyMappings.toFile().getAbsoluteFile()
        def decompilers = loomExtension.decompilerOptions.collect {
            def task = project.tasks.getByName('genSourcesWith' + it.name.capitalize())
            def sourcesPath = task.runtimeJar.get().getAsFile().getAbsolutePath().dropRight(4) + "-sources.jar"
            new FabricLoomModelImpl.DecompilerModelImpl(name: it.name, taskName: task.name, sourcesPath: sourcesPath)
        }

        //noinspection GroovyAssignabilityCheck
        return new FabricLoomModelImpl(tinyMappings, ["single": decompilers], false, [:])
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev FabricLoom project configuration")
    }
}
