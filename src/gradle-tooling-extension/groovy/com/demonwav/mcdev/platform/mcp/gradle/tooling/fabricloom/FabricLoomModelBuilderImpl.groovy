/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
        def tinyMappings = loomExtension.mappingsProvider.tinyMappings.toFile().getAbsoluteFile()
        def decompilers = loomExtension.decompilerOptions.collectEntries {
            def task = project.tasks.getByName('genSourcesWith' + it.name.capitalize())
            def sourcesPath = task.runtimeJar.get().getAsFile().getAbsolutePath().dropRight(4) + "-sources.jar"
            [it.name, sourcesPath]
        }

        //noinspection GroovyAssignabilityCheck
        return new FabricLoomModelImpl(tinyMappings, decompilers)
    }

    @Override
    ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
        return ErrorMessageBuilder.create(
                project, e, "MinecraftDev import errors"
        ).withDescription("Unable to build MinecraftDev FabricLoom project configuration")
    }
}
