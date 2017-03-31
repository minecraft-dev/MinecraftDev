/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.exception.MinecraftSetupException
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import javax.swing.JTextField

abstract class MinecraftModuleWizardStep : ModuleWizardStep() {

    protected fun validate(pluginNameField: JTextField,
                           pluginVersionField: JTextField,
                           mainClassField: JTextField,
                           authorsField: JTextField,
                           dependField: JTextField,
                           pattern: Regex): Boolean {
        try {
            if (pluginNameField.text.trim { it <= ' ' }.isEmpty()) {
                throw MinecraftSetupException("empty", pluginNameField)
            }

            if (pluginVersionField.text.trim { it <= ' ' }.isEmpty()) {
                throw MinecraftSetupException("empty", pluginVersionField)
            }

            if (mainClassField.text.trim { it <= ' ' }.isEmpty()) {
                throw MinecraftSetupException("empty", mainClassField)
            }
            if (!authorsField.text.matches(pattern)) {
                throw MinecraftSetupException("bad", authorsField)
            }

            if (!dependField.text.matches(pattern)) {
                throw MinecraftSetupException("bad", dependField)
            }
        } catch (e: MinecraftSetupException) {
            val message = e.error
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                .setFadeoutTime(4000)
                .createBalloon()
                .show(RelativePoint.getSouthWestOf(e.j), Balloon.Position.below)
            return false
        }

        return true
    }

    companion object {
        val pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?".toRegex()
    }
}
