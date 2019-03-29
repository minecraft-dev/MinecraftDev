/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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

    protected fun validate(
        pluginNameField: JTextField,
        pluginVersionField: JTextField,
        mainClassField: JTextField,
        authorsField: JTextField,
        dependField: JTextField,
        pattern: Regex
    ): Boolean {
        try {
            if (pluginNameField.text.isBlank()) {
                throw EmptyInputSetupException(pluginNameField)
            }

            if (pluginVersionField.text.isBlank()) {
                throw EmptyInputSetupException(pluginVersionField)
            }

            // empty
            if (mainClassField.text.isBlank()) {
                throw EmptyInputSetupException(mainClassField)
            }
            // default package
            if (!mainClassField.text.contains('.')) {
                throw InvalidMainClassNameException(mainClassField)
            }
            // crazy dots
            if (mainClassField.text.split('.').any { it.isEmpty() } ||
                mainClassField.text.first() == '.' || mainClassField.text.last() == '.') {
                throw InvalidMainClassNameException(mainClassField)
            }
            // invalid character
            if (mainClassField.text.split('.').any {
                !it.first().isJavaIdentifierStart() || !it.asSequence().drop(1).all { it.isJavaIdentifierPart() }
            }) {
                throw InvalidMainClassNameException(mainClassField)
            }
            // keyword identifier
            if (mainClassField.text.split('.').any { keywords.contains(it) }) {
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
