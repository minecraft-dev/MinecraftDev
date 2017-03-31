/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.AbstractProjectComponent
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project

class ForgeProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        // assign mcmod.info json thing
        runWriteTaskLater {
            FileTypeManager.getInstance().associate(JsonFileType.INSTANCE, object : FileNameMatcher {
                override fun accept(fileName: String) = fileName == ForgeConstants.MCMOD_INFO
                override fun getPresentableString() = ForgeConstants.MCMOD_INFO
            })
        }
    }
}
