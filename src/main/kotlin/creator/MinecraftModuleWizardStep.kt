/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.exception.SetupException
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import javax.swing.JTextField
import org.apache.commons.lang.WordUtils

abstract class MinecraftModuleWizardStep : ModuleWizardStep() {

    override fun validate(): Boolean {
        try {
            for (field in javaClass.declaredFields) {
                val annotation = field.getAnnotation(ValidatedField::class.java) ?: continue
                field.isAccessible = true
                val textField = field.get(this) as? JTextField ?: continue
                for (validationType in annotation.value) {
                    validationType.validate(textField)
                }
            }
        } catch (e: SetupException) {
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.error, MessageType.ERROR, null)
                .setFadeoutTime(4000)
                .createBalloon()
                .show(RelativePoint.getSouthWestOf(e.j), Balloon.Position.below)
            return false
        }

        return true
    }

    inline fun generateClassName(
        buildSystem: BuildSystem,
        name: String,
        classNameModifier: (String) -> String = { it }
    ): String {
        val packageNameStart = buildSystem.groupId.toPackageName()
        val packageNameEnd = buildSystem.artifactId.toPackageName()
        val className = classNameModifier(name.replace(" ", ""))
        return "$packageNameStart.$packageNameEnd.$className"
    }

    protected fun basicUpdateStep(
        creator: MinecraftProjectCreator,
        pluginNameField: JTextField,
        mainClassField: JTextField
    ) {
        val buildSystem = creator.buildSystem ?: return

        val name = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))
        pluginNameField.text = name

        mainClassField.text = generateClassName(buildSystem, name)
    }
}
