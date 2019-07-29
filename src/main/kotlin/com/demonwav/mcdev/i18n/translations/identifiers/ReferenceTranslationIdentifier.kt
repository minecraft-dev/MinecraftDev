/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations.identifiers

import com.demonwav.mcdev.i18n.translations.Translation
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope

class ReferenceTranslationIdentifier : TranslationIdentifier<PsiReferenceExpression>() {
    override fun identify(element: PsiReferenceExpression): Translation? {
        val reference = element.resolve()
        val statement = element.parent

        if (reference is PsiField) {
            val scope = GlobalSearchScope.allScope(element.project)
            val stringClass =
                JavaPsiFacade.getInstance(element.project).findClass("java.lang.String", scope) ?: return null
            val isConstant =
                reference.hasModifierProperty(PsiModifier.STATIC) && reference.hasModifierProperty(PsiModifier.FINAL)
            val type = reference.type as? PsiClassReferenceType ?: return null
            val resolved = type.resolve() ?: return null
            if (isConstant && (resolved.isEquivalentTo(stringClass) || resolved.isInheritor(stringClass, true))) {
                val referenceElement = reference.initializer as? PsiLiteral ?: return null
                val result = identify(element.project, element, statement, referenceElement)
                return result?.copy(
                    key = result.key.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, ""),
                    varKey = result.varKey.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
                )
            }
        }
        return null
    }

    override fun elementClass(): Class<PsiReferenceExpression> {
        return PsiReferenceExpression::class.java
    }
}
