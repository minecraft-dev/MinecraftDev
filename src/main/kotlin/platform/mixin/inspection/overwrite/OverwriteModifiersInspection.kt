/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.demonwav.mcdev.platform.mixin.util.accessLevel
import com.demonwav.mcdev.platform.mixin.util.findStubMethod
import com.demonwav.mcdev.platform.mixin.util.hasModifier
import com.demonwav.mcdev.platform.mixin.util.internalNameToShortName
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveFirstOverwriteTarget
import com.demonwav.mcdev.util.findAnnotation
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
import org.objectweb.asm.Type

class OverwriteModifiersInspection : OverwriteInspection() {

    override fun getStaticDescription() = "Validates the modifiers of @Overwrite methods"

    override fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation) {
        val psiClass = method.containingClass ?: return
        val targetClasses = psiClass.mixinTargets.ifEmpty { return }

        val target = resolveFirstOverwriteTarget(targetClasses, method) ?: return
        val nameIdentifier = method.nameIdentifier ?: return
        val modifierList = method.modifierList

        // Check access modifiers
        val targetAccessLevel = target.method.accessLevel
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
            if (modifier == PsiModifier.DEFAULT) {
                // default modifier is not present in bytecode
                continue
            }

            val targetModifier = target.method.hasModifier(modifier)
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
                    marker,
                    message,
                    QuickFixFactory.getInstance().createModifierListFix(modifierList, modifier, targetModifier, false)
                )
            }
        }

        val targetAnnotations = target.method.visibleAnnotations ?: mutableListOf()
        target.method.invisibleAnnotations?.let { targetAnnotations += it }
        for (annotation in targetAnnotations) {
            val internalName = Type.getType(annotation.desc).takeIf { it.sort == Type.OBJECT }?.internalName ?: continue
            val qualifiedName = internalName.replace('/', '.').replace('$', '.')
            val overwriteAnnotation = modifierList.findAnnotation(qualifiedName)
            if (overwriteAnnotation == null) {
                val targetAnnPsi = target.method.findStubMethod(target.clazz, method.project)
                    ?.findAnnotation(qualifiedName)
                if (targetAnnPsi != null) {
                    holder.registerProblem(
                        nameIdentifier,
                        "Missing @${internalNameToShortName(internalName)} annotation",
                        AddAnnotationFix(qualifiedName, method, targetAnnPsi.parameterList.attributes)
                    )
                } else {
                    holder.registerProblem(
                        nameIdentifier,
                        "Missing @${internalNameToShortName(internalName)} annotation"
                    )
                }
            }

            // TODO: Check if attributes are specified correctly?
        }
    }
}
