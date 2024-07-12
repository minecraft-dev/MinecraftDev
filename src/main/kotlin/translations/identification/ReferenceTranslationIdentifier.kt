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

package com.demonwav.mcdev.translations.identification

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.resolveToUElement

class ReferenceTranslationIdentifier : TranslationIdentifier<UReferenceExpression>() {
    override fun identify(element: UReferenceExpression): TranslationInstance? {
        val statement = element.uastParent ?: return null
        val project = element.sourcePsi?.project ?: return null
        val reference = element.resolveToUElement() as? UVariable ?: return null
        if (!reference.isFinal) {
            return null
        }

        val resolveScope = element.sourcePsi?.resolveScope ?: return null
        val psiManager = PsiManager.getInstance(project)
        val stringType = PsiType.getJavaLangString(psiManager, resolveScope)
        if (!stringType.isAssignableFrom(reference.type)) {
            return null
        }

        val referenceElement = reference.uastInitializer ?: return null
        val result = identify(project, element, statement, referenceElement) ?: return null

        val infix = result.key.infix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
        return result.copy(key = result.key.copy(infix = infix))
    }

    override fun elementClass(): Class<UReferenceExpression> = UReferenceExpression::class.java
}
