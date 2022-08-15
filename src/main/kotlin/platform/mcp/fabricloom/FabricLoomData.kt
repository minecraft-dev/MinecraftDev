/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import java.io.File

data class FabricLoomData(
    val module: ModuleData,
    val tinyMappings: File?,
    val decompileTasks: Map<String, Set<Decompiler>>,
    val splitMinecraftJar: Boolean
) : AbstractExternalEntityData(module.owner) {

    data class Decompiler(val name: String, val taskName: String, val sourcesPath: String)

    companion object {
        val KEY = Key.create(FabricLoomData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }
}
