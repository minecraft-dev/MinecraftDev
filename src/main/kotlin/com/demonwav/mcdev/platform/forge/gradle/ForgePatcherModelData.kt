/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.platform.forge.gradle.tooling.ForgePatcherModel
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData

class ForgePatcherModelData(val module: ModuleData, val model: ForgePatcherModel) : AbstractExternalEntityData(module.owner) {

    companion object {
        val KEY = Key.create(ForgePatcherModelData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }
}
