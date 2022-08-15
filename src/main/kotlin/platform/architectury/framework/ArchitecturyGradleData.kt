/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.framework

import com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom.ArchitecturyModel
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData

data class ArchitecturyGradleData(
    val module: ModuleData,
    val moduleType: ArchitecturyModel.ModuleType
) : AbstractExternalEntityData(module.owner) {
    companion object {
        val KEY = Key.create(ArchitecturyGradleData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }
}
