/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.platform.canary.util.CanaryConstants
import com.demonwav.mcdev.util.AbstractProjectComponent
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project

class CanaryProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        runWriteTaskLater {
            FileTypeManager.getInstance().associate(PropertiesFileType.INSTANCE, object : FileNameMatcher {
                override fun accept(fileName: String) = fileName == CanaryConstants.CANARY_INF
                override fun getPresentableString() = CanaryConstants.CANARY_INF
            })
        }
    }
}
