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

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.DescReference
import com.demonwav.mcdev.platform.mixin.reference.InjectionPointReference
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair

class UnresolvedReferenceInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports unresolved references in Mixin annotations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            val resolvers: Array<MixinReference> = when (pair.name ?: PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME) {
                "method" -> arrayOf(MethodReference)
                "target" -> arrayOf(TargetReference)
                "value" -> arrayOf(InjectionPointReference, DescReference)
                else -> return
            }

            // Check if valid annotation
            val qualifiedName = pair.annotationFromNameValuePair?.qualifiedName ?: return
            val resolver = resolvers.firstOrNull { it.isValidAnnotation(qualifiedName, pair.project) } ?: return

            val value = pair.value ?: return
            if (value is PsiArrayInitializerMemberValue) {
                for (initializer in value.initializers) {
                    checkResolved(resolver, initializer)
                }
            } else {
                checkResolved(resolver, value)
            }
        }

        private fun checkResolved(resolver: MixinReference, value: PsiAnnotationMemberValue) {
            if (resolver.isUnresolved(value)) {
                holder.registerProblem(
                    value,
                    "Cannot resolve ${resolver.description}".format(value.constantStringValue),
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }
        }
    }
}
