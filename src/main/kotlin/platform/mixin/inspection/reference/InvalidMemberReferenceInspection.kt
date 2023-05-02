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

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNameValuePair

class InvalidMemberReferenceInspection : MixinInspection() {

    override fun getStaticDescription() =
        """
        |Reports invalid usages of member references in Mixin annotations. Two different formats are supported by Mixin:
        | - Lcom/example/ExampleClass;execute(II)V
        | - com.example.ExampleClass.execute(II)V
        """.trimMargin()

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            val name = pair.name ?: return

            val resolver: MixinReference = when (name) {
                "method" -> MethodReference
                "target" -> TargetReference
                else -> return
            }

            // Check if valid annotation
            val qualifiedName = pair.annotationFromNameValuePair?.qualifiedName ?: return
            if (!resolver.isValidAnnotation(qualifiedName, pair.project)) {
                return
            }

            val value = pair.value ?: return

            // Attempt to parse the reference
            when (value) {
                is PsiLiteral -> checkMemberReference(value, value.constantStringValue)
                is PsiArrayInitializerMemberValue -> value.initializers.forEach {
                    checkMemberReference(it, it.constantStringValue)
                }
            }
        }

        private fun checkMemberReference(element: PsiElement, value: String?) {
            val validSelector = value != null &&
                (parseMixinSelector(value, element) != null || isMiscDynamicSelector(element.project, value))
            if (!validSelector) {
                holder.registerProblem(element, "Invalid member reference")
            }
        }
    }
}
