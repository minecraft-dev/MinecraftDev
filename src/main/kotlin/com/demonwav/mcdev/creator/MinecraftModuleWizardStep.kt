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

import com.demonwav.mcdev.exception.BadListSetupException
import com.demonwav.mcdev.exception.EmptyInputSetupException
import com.demonwav.mcdev.exception.SetupException
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
                throw EmptyInputSetupException(pluginNameField)
            }

            if (pluginVersionField.text.trim { it <= ' ' }.isEmpty()) {
                throw EmptyInputSetupException(pluginVersionField)
            }

            if (mainClassField.text.trim { it <= ' ' }.isEmpty()) {
                throw EmptyInputSetupException(mainClassField)
            }
            if (!authorsField.text.matches(pattern)) {
                throw BadListSetupException(authorsField)
            }

            if (!dependField.text.matches(pattern)) {
                throw BadListSetupException(dependField)
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

    companion object {
        val pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?".toRegex()
    }
}
