/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.memberReference
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

class OverwriteTargetInspection : MixinInspection() {

    override fun getStaticDescription() = "Verifies target method of @Overwrites"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val modifiers = method.modifierList

            // Check if the method is an @Overwrite
            modifiers.findAnnotation(MixinConstants.Annotations.OVERWRITE) ?: return

            val identifier = method.nameIdentifier ?: return

            val psiClass = method.containingClass ?: return
            val targets = MixinUtils.getAllMixedClasses(psiClass).values
            if (targets.isEmpty()) {
                return
            }

            val memberReference = method.memberReference

            val resolved = when (targets.size) {
                0 -> return
                1 -> targets.single().findMethods(memberReference).findAny().orElse(null)
                else ->
                    // TODO: Improve detection of valid target methods in Mixins with multiple targets
                    targets.stream()
                            .flatMap { it.findMethods(memberReference) }
                            .findAny().orElse(null)

            }

            if (resolved == null) {
                holder.registerProblem(identifier, "Cannot resolve method '${method.name}' in target class")
            }

            // TODO: Verify method modifiers?
        }
    }

}
