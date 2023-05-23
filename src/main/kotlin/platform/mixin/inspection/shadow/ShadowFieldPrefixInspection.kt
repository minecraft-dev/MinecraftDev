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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.DEFAULT_SHADOW_PREFIX
import com.demonwav.mcdev.util.annotationFromValue
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifierList

class ShadowFieldPrefixInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports @Shadow fields with prefixes"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitField(field: PsiField) {
            val shadow = field.findAnnotation(SHADOW) ?: return
            val prefix = shadow.findDeclaredAttributeValue("prefix")
            if (prefix != null) {
                holder.registerProblem(prefix, "Cannot use prefix for @Shadow fields", QuickFix)
                return
            }

            // Check if field name starts with default shadow prefix
            val fieldName = field.name
            if (fieldName.startsWith(DEFAULT_SHADOW_PREFIX)) {
                holder.registerProblem(
                    field.nameIdentifier,
                    "Cannot use prefix for @Shadow fields",
                    QuickFixFactory.getInstance().createRenameElementFix(
                        field,
                        fieldName.removePrefix(DEFAULT_SHADOW_PREFIX),
                    ),
                )
            }
        }
    }

    private object QuickFix : LocalQuickFix {

        override fun getFamilyName() = "Remove @Shadow prefix"
        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as PsiAnnotationMemberValue

            // Get current prefix name
            val prefixName = element.constantStringValue!!

            // Delete prefix
            val shadow = element.annotationFromValue!!
            runWriteAction {
                shadow.setDeclaredAttributeValue<PsiAnnotationMemberValue>("prefix", null)
            }

            // Rename field (if necessary)
            val field = (shadow.owner as PsiModifierList).parent as PsiField
            val fieldName = field.name
            if (fieldName.startsWith(prefixName)) {
                // Rename field
                QuickFixFactory.getInstance().createRenameElementFix(field, fieldName.removePrefix(prefixName))
                    .applyFix()
            }
        }
    }
}
