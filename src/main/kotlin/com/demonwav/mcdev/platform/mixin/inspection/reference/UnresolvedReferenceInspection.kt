/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.InjectionPointType
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.isWithinDynamicMixin
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
            val resolver: MixinReference = when (name) {
                "method" -> MethodReference
                "target" -> TargetReference
                "value" -> InjectionPointType
                else -> return
            }

            // Check if valid annotation
            val qualifiedName = pair.annotationFromNameValuePair?.qualifiedName ?: return
            if (!resolver.isValidAnnotation(qualifiedName)) {
                return
            }

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
            if (resolver.isUnresolved(value) && !value.isWithinDynamicMixin) {
                holder.registerProblem(
                    value, "Cannot resolve ${resolver.description}".format(value.constantStringValue),
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }
    }
}
