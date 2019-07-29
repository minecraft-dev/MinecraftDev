/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveOverwriteTargets
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod

class OverwriteTargetInspection : OverwriteInspection() {

    override fun getStaticDescription() = "Verifies target method of @Overwrites"

    override fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation) {
        val identifier = method.nameIdentifier ?: return

        val psiClass = method.containingClass ?: return
        val targetClasses = psiClass.mixinTargets.ifEmpty { return }

        val targets = resolveOverwriteTargets(targetClasses, method)
        if (targets.size >= targetClasses.size) {
            // OK, bye
            return
        }

        // TODO: Write quick fix and apply it for OverwriteTargetInspection and ShadowTargetInspection
        holder.registerProblem(
            identifier, "Cannot resolve method '${method.name}' in target class",
            RemoveAnnotationQuickFix(overwrite, method)
        )
    }
}
