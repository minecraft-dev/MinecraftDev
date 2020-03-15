/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor

class MixinSuperClassInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports invalid @Mixin super classes."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            if (!psiClass.isMixin) {
                return
            }

            val superClass = psiClass.superClass ?: return
            if (superClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                return
            }

            val targetClasses = psiClass.mixinTargets.ifEmpty { return }

            val superTargets = superClass.mixinTargets
            if (superTargets.isEmpty()) {
                // Super class must be a regular class in the hierarchy of the target class(es)
                for (targetClass in targetClasses) {
                    if (targetClass equivalentTo superClass) {
                        reportSuperClass(psiClass, "Cannot extend target class")
                    } else if (!targetClass.isInheritor(superClass, true)) {
                        reportSuperClass(
                            psiClass,
                            "Cannot find '${superClass.shortName}' " +
                                "in the hierarchy of target class '${targetClass.shortName}'"
                        )
                    }
                }
            } else {
                // At least one of the target classes of the super mixin must be in the hierarchy of the target class(es)
                for (targetClass in targetClasses) {
                    if (!superTargets.any { superTarget -> targetClass.isInheritor(superTarget, true) }) {
                        reportSuperClass(
                            psiClass,
                            "Cannot find '${targetClass.shortName}' in the hierarchy of the super mixin"
                        )
                    }
                }
            }
        }

        private fun reportSuperClass(psiClass: PsiClass, description: String) {
            holder.registerProblem(psiClass.extendsList?.referenceElements?.firstOrNull() ?: psiClass, description)
        }
    }
}
