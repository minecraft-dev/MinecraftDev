package com.demonwav.mcdev.platform.placeholderapi

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object PlaceholderApiTemplate {

    fun applyMainClassTemplate(
        project: Project,
        mainClassFile: VirtualFile,
        packageName: String,
        className: String
    ) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(
            project,
            mainClassFile,
            MinecraftFileTemplateGroupFactory.PLACEHOLDERAPI_MAIN_CLASS_TEMPLATE,
            properties
        )
    }
}
