/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveFirstOverwriteTarget
import com.demonwav.mcdev.util.isAccessModifier
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier

class OverwriteModifiersInspection : OverwriteInspection() {

    override fun getStaticDescription() = "Validates the modifiers of @Overwrite methods"

    override fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation) {
        val psiClass = method.containingClass ?: return
        val targetClasses = psiClass.mixinTargets
        if (targetClasses.isEmpty()) {
            return
        }

        val target = resolveFirstOverwriteTarget(targetClasses, method)?.modifierList ?: return
        val modifierList = method.modifierList

        for (modifier in PsiModifier.MODIFIERS) {
            val targetModifier = target.hasModifierProperty(modifier)
            val overwriteModifier = modifierList.hasModifierProperty(modifier)
            if (targetModifier != overwriteModifier) {
                if (!targetModifier && isAccessModifier(modifier)) {
                    // Don't attempt to remove access modifiers
                    continue
                }

                val message = if (targetModifier) {
                    "'$modifier' is not present on target method"
                } else {
                    "'$modifier' modifier does not match target method"
                }

                holder.registerProblem(modifierList, message,
                    QuickFixFactory.getInstance().createModifierListFix(modifierList, modifier, targetModifier, false))
            }
        }

        for (annotation in target.annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue
            val overwriteAnnotation = modifierList.findAnnotation(qualifiedName)
            if (overwriteAnnotation == null) {
                holder.registerProblem(modifierList, "Missing @${annotation.nameReferenceElement?.text} annotation",
                    AddAnnotationFix(qualifiedName, method, annotation.parameterList.attributes))
            }

            // TODO: Check if attributes are specified correctly?
        }
    }
}
