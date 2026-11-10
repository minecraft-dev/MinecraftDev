/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings

/**
 * Annotater requires fully qualified class names in Javadoc references, so make completion insert them.
 */
class AnnotaterJavadocCodeStyleSettingsModifier : CodeStyleSettingsModifier {
    override fun modifySettings(settings: TransientCodeStyleSettings, file: PsiFile): Boolean {
        if (AnnotaterJavadocVirtualFile.of(file) == null) {
            return false
        }
        settings.getCustomSettings(JavaCodeStyleSettings::class.java).CLASS_NAMES_IN_JAVADOC =
            JavaCodeStyleSettings.FULLY_QUALIFY_NAMES_ALWAYS
        return true
    }

    override fun getName() = "Annotater"

    override fun getStatusBarUiContributor(settings: TransientCodeStyleSettings): CodeStyleStatusBarUIContributor? =
        null
}
