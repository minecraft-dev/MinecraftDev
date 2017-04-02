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

        for (modifier in PsiModifier.MODIFIERS) {
            val targetModifier = target.hasModifierProperty(modifier)
            val overwriteModifier = modifierList.hasModifierProperty(modifier)
            if (targetModifier != overwriteModifier) {
                val target: PsiElement
                val message = if (isAccessModifier(modifier)) {
                    if (!targetModifier) {
                        // Don't attempt to remove access modifiers
                        continue
                    }

                    val currentModifier = PsiUtil.getAccessModifier(PsiUtil.getAccessLevel(modifierList))
                    target = modifierList.findKeyword(currentModifier) ?: nameIdentifier
                    "Invalid access modifiers, has: $currentModifier, but target has: $overwriteModifier"
                } else if (targetModifier) {
                    target = nameIdentifier
                    "Method must be '$modifier'"
                } else {
                    target = modifierList.findKeyword(modifier) ?: nameIdentifier
                    "'$modifier' modifier does not match target method"
                }

                holder.registerProblem(target, message,
                    QuickFixFactory.getInstance().createModifierListFix(modifierList, modifier, targetModifier, false))
            }
        }

        for (annotation in target.annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue
            val overwriteAnnotation = modifierList.findAnnotation(qualifiedName)
            if (overwriteAnnotation == null) {
                holder.registerProblem(nameIdentifier, "Missing @${annotation.nameReferenceElement?.text} annotation",
                    AddAnnotationFix(qualifiedName, method, annotation.parameterList.attributes))
            }

            // TODO: Check if attributes are specified correctly?
        }
    }
}
