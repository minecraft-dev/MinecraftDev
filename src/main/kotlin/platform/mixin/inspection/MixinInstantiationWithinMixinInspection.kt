/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.action.insertShadows
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiConstructorCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression

class MixinInstantiationWithinMixinInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when a mixin class is instantiated within that same class using a constructor which doesn't exist in the target class"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitNewExpression(expression: PsiNewExpression) {
            if (expression.isArrayCreation || expression.anonymousClass != null) {
                return
            }
            val classReference = expression.classReference ?: return
            val constructedClass = classReference.resolve() as? PsiClass ?: return
            visitConstructorCall(expression, constructedClass, classReference) { LocalQuickFix.EMPTY_ARRAY }
        }

        override fun visitEnumConstant(enumConstant: PsiEnumConstant) {
            val constructedClass = enumConstant.containingClass ?: return
            visitConstructorCall(enumConstant, constructedClass, enumConstant.nameIdentifier) {
                var possibleCtors: MutableMap<String, PsiMethod>? = null
                for (mixinTarget in constructedClass.mixinTargets) {
                    if (possibleCtors == null) {
                        possibleCtors = mutableMapOf()
                        for (method in mixinTarget.methods) {
                            if (method.isConstructor) {
                                possibleCtors[method.desc] = method.findOrConstructSourceMethod(mixinTarget, holder.project, canDecompile = false)
                            }
                        }
                    } else {
                        possibleCtors.keys.retainAll(mixinTarget.methods.mapNotNullTo(mutableSetOf()) { method -> method.desc.takeIf { method.isConstructor } })
                    }
                }
                if (possibleCtors.isNullOrEmpty()) {
                    return@visitConstructorCall LocalQuickFix.EMPTY_ARRAY
                }
                possibleCtors.map { (desc, sourceMethod) ->
                    val presentableParams =
                        sourceMethod.parameterList.parameters.joinToString { it.type.presentableText }
                    AddShadowConstructorFix(constructedClass, desc, "($presentableParams)")
                }.toTypedArray()
            }
        }

        private fun visitConstructorCall(
            constructorCall: PsiConstructorCall,
            constructedClass: PsiClass,
            problemElement: PsiElement,
            quickFixGenerator: () -> Array<LocalQuickFix>,
        ) {
            if (!constructedClass.isMixin || constructedClass != constructorCall.findContainingClass()) {
                return
            }
            val resolvedConstructor = constructorCall.resolveConstructor()
            if (resolvedConstructor == null && (constructorCall.argumentList?.isEmpty == false || constructedClass.constructors.isNotEmpty())) {
                // don't generate errors when there is an unresolved constructor compile error
                return
            }
            val resolvedConstructorDesc = resolvedConstructor?.descriptor ?: when {
                constructedClass.isEnum -> "(Ljava/lang/String;)V"
                !constructedClass.hasModifierProperty(PsiModifier.STATIC) && constructedClass.containingClass != null ->
                    "(${constructedClass.containingClass!!.descriptor})V"
                else -> "()V"
            }

            val constructorExistsInAllTargets = constructedClass.mixinTargets.all {
                it.findMethod(
                    MemberReference(
                        "<init>",
                        resolvedConstructorDesc
                    )
                ) != null
            }
            if (constructorExistsInAllTargets) {
                return
            }

            holder.registerProblem(problemElement, "Constructor does not exist in target class", *quickFixGenerator())
        }
    }

    private class AddShadowConstructorFix(constructedClass: PsiClass, private val desc: String, private val presentableParams: String) : LocalQuickFixOnPsiElement(constructedClass) {
        override fun getFamilyName() = "Add shadow constructor with parameters $presentableParams"
        override fun getText() = "Add shadow constructor with parameters $presentableParams"

        override fun invoke(project: Project, psiFile: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val constructedClass = startElement as? PsiClass ?: return
            val arbitraryMixinTarget = constructedClass.mixinTargets.firstOrNull() ?: return
            val matchingConstructor = arbitraryMixinTarget.findMethod(MemberReference("<init>", desc)) ?: return
            val sourceConstructor = matchingConstructor.findOrConstructSourceMethod(arbitraryMixinTarget, project, canDecompile = false)
            insertShadows(project, constructedClass, sequenceOf(sourceConstructor))
        }
    }
}
