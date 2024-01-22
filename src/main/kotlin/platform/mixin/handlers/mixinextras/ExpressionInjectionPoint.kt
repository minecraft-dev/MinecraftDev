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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.NavigationVisitor
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.findContainingModifierList
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.objectweb.asm.tree.ClassNode

class ExpressionInjectionPoint : InjectionPoint<PsiElement>() {
    override fun onCompleted(editor: Editor, reference: PsiLiteral) {
        val modifierList = reference.findContainingModifierList() ?: return
        if (modifierList.hasAnnotation(MixinConstants.MixinExtras.EXPRESSION)) {
            return
        }

        val project = reference.project

        val exprAnnotation = modifierList.addAfter(
            JavaPsiFacade.getElementFactory(project)
                .createAnnotationFromText("@${MixinConstants.MixinExtras.EXPRESSION}(\"\")", reference),
            null
        )

        // add imports and reformat
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(exprAnnotation)
        JavaCodeStyleManager.getInstance(project).optimizeImports(modifierList.containingFile)
        val formattedModifierList = CodeStyleManager.getInstance(project).reformat(modifierList) as PsiModifierList

        // move the caret to @Expression("<caret>")
        val formattedExprAnnotation = formattedModifierList.findAnnotation(MixinConstants.MixinExtras.EXPRESSION)
            ?: return
        val exprLiteral = formattedExprAnnotation.findDeclaredAttributeValue(null) ?: return
        editor.caretModel.moveToOffset(exprLiteral.textRange.startOffset + 1)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        return null
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement>? {
        return null
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>
    ): LookupElementBuilder? {
        return null
    }
}
