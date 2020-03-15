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
import com.demonwav.mcdev.platform.mixin.util.resolveFirstOverwriteTarget
import com.demonwav.mcdev.util.findKeyword
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.isAccessModifier
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiUtil

class OverwriteModifiersInspection : OverwriteInspection() {

    override fun getStaticDescription() = "Validates the modifiers of @Overwrite methods"

    override fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation) {
        val psiClass = method.containingClass ?: return
        val targetClasses = psiClass.mixinTargets.ifEmpty { return }

        val target = resolveFirstOverwriteTarget(targetClasses, method)?.modifierList ?: return
        val nameIdentifier = method.nameIdentifier ?: return
        val modifierList = method.modifierList

        // Check access modifiers
        val targetAccessLevel = PsiUtil.getAccessLevel(target)
        val currentAccessLevel = PsiUtil.getAccessLevel(modifierList)
        if (currentAccessLevel < targetAccessLevel) {
            val targetModifier = PsiUtil.getAccessModifier(targetAccessLevel)
            val currentModifier = PsiUtil.getAccessModifier(currentAccessLevel)
            holder.registerProblem(
                modifierList.findKeyword(currentModifier) ?: nameIdentifier,
                "$currentModifier @Overwrite cannot reduce visibility of " +
                    "${PsiUtil.getAccessModifier(targetAccessLevel)} target method",
                QuickFixFactory.getInstance().createModifierListFix(modifierList, targetModifier, true, false)
            )
        }

        for (modifier in PsiModifier.MODIFIERS) {
            if (isAccessModifier(modifier)) {
                // Access modifiers are already checked above
                continue
            }

            val targetModifier = target.hasModifierProperty(modifier)
            val overwriteModifier = modifierList.hasModifierProperty(modifier)
            if (targetModifier != overwriteModifier) {
                val marker: PsiElement
                val message = if (targetModifier) {
                    marker = nameIdentifier
                    "Method must be '$modifier'"
                } else {
                    marker = modifierList.findKeyword(modifier) ?: nameIdentifier
                    "'$modifier' modifier does not match target method"
                }

                holder.registerProblem(
                    marker, message,
                    QuickFixFactory.getInstance().createModifierListFix(modifierList, modifier, targetModifier, false)
                )
            }
        }

        for (annotation in target.annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue
            val overwriteAnnotation = modifierList.findAnnotation(qualifiedName)
            if (overwriteAnnotation == null) {
                holder.registerProblem(
                    nameIdentifier, "Missing @${annotation.nameReferenceElement?.text} annotation",
                    AddAnnotationFix(qualifiedName, method, annotation.parameterList.attributes)
                )
            }

            // TODO: Check if attributes are specified correctly?
        }
    }
}
