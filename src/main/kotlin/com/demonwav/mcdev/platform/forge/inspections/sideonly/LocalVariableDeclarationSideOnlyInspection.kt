/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.platform.forge.sided.Side
import com.demonwav.mcdev.platform.forge.sided.SidedClassCache
import com.demonwav.mcdev.platform.forge.sided.SidedMethodCache
import com.demonwav.mcdev.platform.forge.sided.getInferenceReason
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLocalVariable

class LocalVariableDeclarationSideOnlyInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getDisplayName() = "Invalid usage of local variable declaration annotated with @SideOnly"

    override fun getStaticDescription() =
        "A variable whose class declaration is annotated with @SideOnly for one side cannot be declared in a class" +
        " or method that does not match the same side."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        private val project = holder.project
        private val classCache = SidedClassCache.getInstance(project)
        private val methodCache = SidedMethodCache.getInstance(project)

        override fun visitLocalVariable(variable: PsiLocalVariable) {
            if (!SideOnlyUtil.beginningCheck(variable)) {
                return
            }

            val method = variable.findContainingMethod() ?: return
            val psiClass = variable.findContainingClass() ?: return
            val variableClass = (variable.type as? PsiClassType)?.resolve()  ?: return

            val methodState = methodCache.getSideState(method)
            val psiClassState = classCache.getSideState(psiClass)
            val variableClassState = classCache.getSideState(variableClass)

            if (variableClassState == null || variableClassState.side === Side.INVALID) {
                return
            }

            val variableClassInference = getInferenceReason(variableClassState, variableClass.shortName, project)
            val variableClassInferenceText = if (variableClassInference == null) {
                ""
            } else {
                "\nLocal variable ${variable.name} side inferred from class: $variableClassInference"
            }

            if (methodState != null && methodState.side !== Side.INVALID) {
                if (methodState.side !== variableClassState.side) {
                    val methodInference = getInferenceReason(methodState, method.name, project)

                    val text = buildString {
                        append("A local variable of class with side of ").append(variableClassState.side.annotation)
                        append(" cannot be used in a method with side of ").append(methodState.side.annotation)
                        append(variableClassInferenceText)
                        methodInference?.let { append('\n').append(it) }
                    }

                    holder.registerProblem(variable, text, ProblemHighlightType.ERROR)
                }
            } else if (methodState == null) {
                val text = buildString {
                    append("A local variable of class with side of ").append(variableClassState.side.annotation)
                    append(" cannot be used in an un-annotated method")
                    append(variableClassInferenceText)
                }

                holder.registerProblem(variable, text, ProblemHighlightType.ERROR)
            }

            if (psiClassState != null && psiClassState.side !== Side.INVALID) {
                if (psiClassState.side !== variableClassState.side) {
                    val psiClassInference = getInferenceReason(psiClassState, psiClass.shortName, project)

                    val text = buildString {
                        append("A local variable of class with side of ").append(variableClassState.side.annotation)
                        append(" cannot be used in a class with side of ").append(psiClassState.side.annotation)
                        append(variableClassInferenceText)
                        psiClassInference?.let { append('\n').append(it) }
                    }

                    holder.registerProblem(variable, text, ProblemHighlightType.ERROR)
                }
            }
        }
    }
}
