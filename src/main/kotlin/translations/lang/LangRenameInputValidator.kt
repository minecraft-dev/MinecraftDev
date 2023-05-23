/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext

class LangRenameInputValidator : RenameInputValidatorEx {
    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext) = !newName.contains('=')

    override fun getPattern(): ElementPattern<out PsiElement> = PlatformPatterns.psiElement(LangEntry::class.java)

    override fun getErrorMessage(newName: String, project: Project) =
        if (newName.contains('=')) "Key must not contain separator character ('=')" else null
}
