/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.platform.mixin.util.accessLevel
import com.demonwav.mcdev.platform.mixin.util.findStubMethod
import com.demonwav.mcdev.platform.mixin.util.hasModifier
import com.demonwav.mcdev.platform.mixin.util.internalNameToShortName
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findKeyword
import com.demonwav.mcdev.util.isAccessModifier
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemHighlightType
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
        val overwriteAnnotation = method.getAnnotation(OVERWRITE) ?: return
        val target = (MixinAnnotationHandler.resolveTarget(overwriteAnnotation).firstOrNull() as? MethodTargetMember)
            ?.classAndMethod ?: return
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
                QuickFixFactory.getInstance().createModifierListFix(modifierList, targetModifier, true, false),
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
            if (modifier == PsiModifier.OPEN || modifier == PsiModifier.TRANSITIVE) {
                // those are only used in module-info
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
                    QuickFixFactory.getInstance().createModifierListFix(modifierList, modifier, targetModifier, false),
                )
            }
        }

        val targetAnnotations = target.method.visibleAnnotations ?: mutableListOf()
        target.method.invisibleAnnotations?.let { targetAnnotations += it }
        for (annotation in targetAnnotations) {
            val internalName = Type.getType(annotation.desc).takeIf { it.sort == Type.OBJECT }?.internalName ?: continue
            val qualifiedName = internalName.replace('/', '.').replace('$', '.')
            val annotationOnOverwrite = modifierList.findAnnotation(qualifiedName)
            if (annotationOnOverwrite == null) {
                val targetAnnPsi = target.method.findStubMethod(target.clazz, method.project)
                    ?.findAnnotation(qualifiedName)
                if (targetAnnPsi != null) {
                    holder.registerProblem(
                        nameIdentifier,
                        "Missing @${internalNameToShortName(internalName)} annotation",
                        ProblemHighlightType.WARNING,
                        AddAnnotationFix(qualifiedName, method, targetAnnPsi.parameterList.attributes),
                    )
                } else {
                    holder.registerProblem(
                        nameIdentifier,
                        "Missing @${internalNameToShortName(internalName)} annotation",
                        ProblemHighlightType.WARNING,
                    )
                }
            }

            // TODO: Check if attributes are specified correctly?
        }
    }
}
