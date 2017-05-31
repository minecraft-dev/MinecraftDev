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
import com.demonwav.mcdev.exception.InvalidMainClassNameException
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

            if (mainClassField.text.trim { it <= ' ' }.isEmpty()) { // empty
                throw EmptyInputSetupException(mainClassField)
            }
            if (!mainClassField.text.contains('.')) { // default package
                throw InvalidMainClassNameException(mainClassField)
            }
            if (mainClassField.text.contains("\\s+".toRegex())) { // whitespace
                throw InvalidMainClassNameException(mainClassField)
            }
            if (mainClassField.text.first().isJavaIdentifierStart() && // invalid character
                mainClassField.text.asSequence().drop(1).all { it.isJavaIdentifierPart() }) {
                throw InvalidMainClassNameException(mainClassField)
            }
            if (mainClassField.text.first() == '.') { // idk why this doesn't fail in the above check, but w/e
                throw InvalidMainClassNameException(mainClassField)
            }
            if (mainClassField.text.split('.').any { keywords.contains(it) }) { // keyword identifier
                throw InvalidMainClassNameException(mainClassField)
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
        val keywords = setOf(
            "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean", "do", "if",
            "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case",
            "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
            "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"
        )
    }
}
