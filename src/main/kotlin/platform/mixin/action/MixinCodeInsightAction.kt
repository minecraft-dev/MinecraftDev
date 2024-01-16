/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.actions.SimpleCodeInsightAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class MixinCodeInsightAction : SimpleCodeInsightAction() {

    override fun startInWriteAction() = false

    // Display action in Mixin classes only
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language != JavaLanguage.INSTANCE) {
            return false
        }

        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        val containingClass = element.findContainingClass() ?: return false
        return containingClass.isMixin
    }
}
