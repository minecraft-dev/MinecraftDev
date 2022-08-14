/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.findStubClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.util.fullQualifiedName
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

            val superInternalName = superClass.fullQualifiedName?.replace('.', '/') ?: return

            val targetClasses = psiClass.mixinTargets.ifEmpty { return }

            val superTargets = superClass.mixinTargets
            if (superTargets.isEmpty()) {
                // Super class must be a regular class in the hierarchy of the target class(es)
                for (targetClass in targetClasses) {
                    if (targetClass.name == superInternalName) {
                        reportSuperClass(psiClass, "Cannot extend target class")
                    } else if (targetClass.findStubClass(psiClass.project)?.isInheritor(superClass, true) == false) {
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
                    val targetStub = targetClass.findStubClass(psiClass.project) ?: continue
                    if (!superTargets.asSequence()
                        .map { it.findStubClass(psiClass.project) }
                        .any { superTarget -> superTarget == null || targetStub.isInheritor(superTarget, true) }
                    ) {
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
