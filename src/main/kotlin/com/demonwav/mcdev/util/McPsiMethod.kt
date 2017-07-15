/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiUtil

fun PsiMethod.isCalling(reference: PsiMethod?, paramIndex: Int, referenceParamIndex: Int): Boolean {
    if (this === reference && paramIndex == referenceParamIndex) {
        return true
    }
    if (reference == null || paramIndex == -1) {
        return false
    }
    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        elem as? PsiMethodCallExpression ?: elem.children.map { findFirstMethodCall(it) }.find { it != null }

    val value = findFirstMethodCall(this)
    return value?.checkForReference(this, reference, paramIndex, referenceParamIndex) { it.isCalling(reference, paramIndex, referenceParamIndex) } ?: false
}

fun PsiMethod.isReturningResultOf(reference: PsiMethod?, paramIndex: Int, referenceParamIndex: Int): Boolean {
    if (this === reference && paramIndex == referenceParamIndex) {
        return true
    }
    if (reference == null || paramIndex == -1) {
        return false
    }
    if (this.isConstructor || reference.isConstructor) {
        return this.isConstructingType(reference, paramIndex, referenceParamIndex)
    }
    if (this.returnType == null || reference.returnType == null) {
        return false
    }
    if (!this.returnType!!.isAssignableFrom(reference.returnType!!)) {
        return false
    }
    for (returnStatement in PsiUtil.findReturnStatements(this)) {
        val value = returnStatement.returnValue
        if (value is PsiMethodCallExpression) {
            val ref = value.referencedMethod
            if (ref === reference) {
                val param = value.argumentList.expressions[referenceParamIndex]
                if (param is PsiReferenceExpression) {
                    val paramRef = param.advancedResolve(false).element
                    return paramRef === this.parameterList.parameters[paramIndex]
                } else if (param is PsiPolyadicExpression) {
                    for (operand in param.operands)
                        if (operand is PsiReferenceExpression) {
                            val operandRef = operand.advancedResolve(false).element
                            return operandRef === this.parameterList.parameters[paramIndex]
                        }
                } else {
                    return param === value.argumentList.expressions[paramIndex]
                }
            } else if (ref != null) {
                return ref.isReturningResultOf(reference, paramIndex, referenceParamIndex)
            }
        }
    }
    return false
}

fun PsiMethod.isConstructingType(reference: PsiMethod?, paramIndex: Int, referenceParamIndex: Int): Boolean {
    if (this === reference && paramIndex == referenceParamIndex) {
        return true
    }
    if (reference == null || paramIndex == -1) {
        return false
    }
    if (!this.isConstructor || !reference.isConstructor) {
        return false
    }
    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        if (elem is PsiMethodCallExpression &&
            (elem.methodExpression.text == "super" || elem.methodExpression.text == "this"))
            elem
        else
            elem.children.map { findFirstMethodCall(it) }.find { it != null }

    val value = findFirstMethodCall(this)
    return value?.checkForReference(this, reference, paramIndex, referenceParamIndex) { it.isConstructingType(reference, paramIndex, referenceParamIndex) } ?: false
}