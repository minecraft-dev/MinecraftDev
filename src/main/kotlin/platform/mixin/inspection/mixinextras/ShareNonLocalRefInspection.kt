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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isLocalRef
import com.demonwav.mcdev.platform.mixin.util.wrapLocalRef
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class ShareNonLocalRefInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.MixinExtras.SHARE)) {
                return
            }

            val parameter = annotation.parent?.parent as? PsiParameter ?: return
            val typeElement = parameter.typeElement ?: return

            if (!parameter.type.isLocalRef) {
                holder.registerProblem(
                    typeElement,
                    "@Share parameter must be of type LocalRef",
                    ReplaceWithLocalRefFix(typeElement),
                )
            }
        }
    }

    private class ReplaceWithLocalRefFix(typeElement: PsiTypeElement) : LocalQuickFixOnPsiElement(typeElement) {
        override fun getText() = "Replace with LocalRef"

        override fun getFamilyName() = "Replace with LocalRef"

        override fun invoke(
            project: Project,
            file: PsiFile,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val typeElement = startElement as? PsiTypeElement ?: return
            val newTypeElement =
                JavaPsiFacade.getElementFactory(project).createTypeElement(typeElement.type.wrapLocalRef(project))
            val elementToReformat = typeElement.replace(newTypeElement)
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(elementToReformat)
        }
    }
}
