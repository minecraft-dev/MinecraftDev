/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ModifyVariableArgsOnlyInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                val modifyVariable = method.findAnnotation(MODIFY_VARIABLE) ?: return
                if (modifyVariable.findDeclaredAttributeValue("argsOnly")?.constantValue == true) {
                    return
                }
                val ordinal = (modifyVariable.findDeclaredAttributeValue("ordinal")?.constantValue as? Int?)
                    ?.takeIf { it != -1 }
                val index = (modifyVariable.findDeclaredAttributeValue("index")?.constantValue as? Int?)
                    ?.takeIf { it != -1 }
                if (ordinal == null && index == null && modifyVariable.findDeclaredAttributeValue("name") != null) {
                    return
                }
                val wantedType = method.parameterList.getParameter(0)?.type?.descriptor ?: return
                val problemElement = modifyVariable.nameReferenceElement ?: return

                val handler = MixinAnnotationHandler.forMixinAnnotation(MODIFY_VARIABLE) ?: return
                val targets = handler.resolveTarget(modifyVariable).ifEmpty { return }
                val methodTargets = targets.asSequence()
                    .filterIsInstance<MethodTargetMember>()
                    .map { it.classAndMethod }
                for ((targetClass, targetMethod) in methodTargets) {
                    val argTypes = mutableListOf<String?>()
                    if (!targetMethod.hasAccess(Opcodes.ACC_STATIC)) {
                        argTypes += "L${targetClass.name};"
                    }
                    for (arg in Type.getArgumentTypes(targetMethod.desc)) {
                        argTypes += arg.descriptor
                        if (arg.size == 2) {
                            argTypes += null
                        }
                    }

                    if (ordinal != null) {
                        if (argTypes.asSequence().filter { it == wantedType }.count() <= ordinal) {
                            return
                        }
                    } else if (index != null) {
                        if (argTypes.size <= index) {
                            return
                        }
                    } else {
                        if (argTypes.asSequence().filter { it == wantedType }.count() != 1) {
                            return
                        }
                    }
                }

                val description = "ModifyVariable may be argsOnly = true"
                holder.registerProblem(problemElement, description, AddArgsOnlyFix(modifyVariable))
            }
        }
    }

    override fun getStaticDescription() =
        "Checks that ModifyVariable has argsOnly if it targets arguments, which improves performance of the mixin"

    private class AddArgsOnlyFix(annotation: PsiAnnotation) : LocalQuickFixOnPsiElement(annotation) {
        override fun getFamilyName() = "Add argsOnly = true"
        override fun getText() = "Add argsOnly = true"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val annotation = startElement as? PsiAnnotation ?: return
            val trueExpr = JavaPsiFacade.getElementFactory(project).createLiteralExpression(true)
            annotation.setDeclaredAttributeValue("argsOnly", trueExpr)
        }
    }
}
