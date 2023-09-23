/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.util

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.TypeConstraint
import com.intellij.codeInspection.dataFlow.interpreter.RunnerResult
import com.intellij.codeInspection.dataFlow.interpreter.StandardDataFlowInterpreter
import com.intellij.codeInspection.dataFlow.java.ControlFlowAnalyzer
import com.intellij.codeInspection.dataFlow.java.anchor.JavaExpressionAnchor
import com.intellij.codeInspection.dataFlow.jvm.JvmDfaMemoryStateImpl
import com.intellij.codeInspection.dataFlow.jvm.descriptors.PlainDescriptor
import com.intellij.codeInspection.dataFlow.lang.DfaAnchor
import com.intellij.codeInspection.dataFlow.lang.DfaListener
import com.intellij.codeInspection.dataFlow.memory.DfaMemoryState
import com.intellij.codeInspection.dataFlow.types.DfReferenceType
import com.intellij.codeInspection.dataFlow.value.DfaValue
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstantInitializer
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.util.PsiUtil

object McdevDfaUtil {
    // Copy of package-private DfaUtil.getDataflowContext
    fun getDataflowContext(expression: PsiExpression): PsiElement? {
        var element = expression.parent
        while (true) {
            if (element == null || element is PsiAnnotation) {
                return null
            }
            if (element is PsiMethod && !element.isConstructor) {
                val containingClass = element.containingClass
                if (containingClass != null &&
                    (!PsiUtil.isLocalOrAnonymousClass(containingClass) || containingClass is PsiEnumConstantInitializer)
                ) {
                    return element.body
                }
            }
            if (element is PsiClass && !PsiUtil.isLocalOrAnonymousClass(element)) {
                return element
            }
            element = element.parent
        }
    }

    fun isSometimesEqualToParameter(expression: PsiExpression, parameter: PsiParameter): Boolean {
        val block = getDataflowContext(expression) ?: return false
        val factory = DfaValueFactory(expression.project)
        val flow = ControlFlowAnalyzer.buildFlow(block, factory, true) ?: return false

        val memState = JvmDfaMemoryStateImpl(factory)
        val stableParam = PlainDescriptor.createVariableValue(
            factory,
            LightParameter("stableParam", parameter.type, block)
        )
        val param = PlainDescriptor.createVariableValue(factory, parameter)
        memState.applyCondition(stableParam.eq(param))

        var isSometimesEqual = false

        val interpreter = StandardDataFlowInterpreter(
            flow,
            object : DfaListener {
                override fun beforePush(
                    args: Array<out DfaValue>,
                    value: DfaValue,
                    anchor: DfaAnchor,
                    state: DfaMemoryState
                ) {
                    if (anchor is JavaExpressionAnchor && anchor.expression equivalentTo expression) {
                        if (state.areEqual(stableParam, value)) {
                            isSometimesEqual = true
                        }
                    }
                }
            }
        )

        if (interpreter.interpret(memState) != RunnerResult.OK) {
            return false
        }

        return isSometimesEqual
    }

    fun getDataflowType(expression: PsiExpression): PsiType? {
        return tryGetDataflowType(expression) ?: expression.type
    }

    fun tryGetDataflowType(expression: PsiExpression): PsiType? {
        return getTypeConstraint(expression)?.getPsiType(expression.project)
    }

    fun getTypeConstraint(expression: PsiExpression): TypeConstraint? {
        return (CommonDataflow.getDfType(expression) as? DfReferenceType)?.constraint
    }
}
