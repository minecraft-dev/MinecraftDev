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
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair

class InvalidMemberReferenceInspection : MixinInspection() {

    override fun getStaticDescription() = """
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
            val annotation = pair.annotationFromNameValuePair ?: return
            if (!resolver.isValidAnnotation(annotation.qualifiedName!!)) {
                return
            }

            val value = pair.value ?: return

            // Attempt to parse the reference
            if (MixinMemberReference.parse(value.constantStringValue) == null) {
                holder.registerProblem(value, "Invalid member reference")
            }
        }
    }
}
