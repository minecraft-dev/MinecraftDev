/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
            val name = pair.name ?: PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME
            val resolvers: Array<MixinReference> = when (name) {
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
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }
    }
}
