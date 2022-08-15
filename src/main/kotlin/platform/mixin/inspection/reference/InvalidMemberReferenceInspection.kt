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
