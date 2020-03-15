/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.quickfix.RemoveAnnotationAttributeQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

class UnusedShadowMethodPrefixInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports unused prefixes of @Shadow methods"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val shadow = method.findAnnotation(SHADOW) ?: return
            val prefixValue = shadow.findDeclaredAttributeValue("prefix") ?: return
            val prefix = prefixValue.constantStringValue ?: return

            if (!method.name.startsWith(prefix)) {
                holder.registerProblem(
                    prefixValue,
                    "Unused @Shadow prefix",
                    RemoveAnnotationAttributeQuickFix("@Shadow", "prefix")
                )
            }
        }
    }
}
