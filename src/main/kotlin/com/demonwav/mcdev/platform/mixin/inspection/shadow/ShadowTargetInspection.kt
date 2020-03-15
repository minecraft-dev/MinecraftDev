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
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveShadowTargets
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierList

class ShadowTargetInspection : MixinInspection() {

    override fun getStaticDescription() = "Validates targets of @Shadow members"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (annotation.qualifiedName != SHADOW) {
                return
            }

            val modifierList = annotation.owner as? PsiModifierList ?: return
            val member = modifierList.parent as? PsiMember ?: return
            val psiClass = member.containingClass ?: return
            val targetClasses = psiClass.mixinTargets.ifEmpty { return }

            val targets = resolveShadowTargets(annotation, targetClasses, member) ?: return
            if (targets.size >= targetClasses.size) {
                // Everything is fine, bye
                return
            }

            // Oh :(, maybe we can help? (TODO: Maybe later)
            // Write quick fix and apply it for OverwriteTargetInspection and ShadowTargetInspection
            holder.registerProblem(
                annotation, "Cannot resolve member '${member.name}' in target class",
                RemoveAnnotationQuickFix(annotation, member)
            )
        }
    }
}
