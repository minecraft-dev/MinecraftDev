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

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.util.PsiUtil

val PsiCall.referencedMethod: PsiMethod?
    get() = when (this) {
        is PsiMethodCallExpression -> this.methodExpression.advancedResolve(false).element as PsiMethod?
        is PsiNewExpression -> this.resolveMethod()
        else -> null
    }

inline fun PsiMethodCallExpression.checkForReference(method: PsiMethod, reference: PsiMethod?, paramIndex: Int, referenceParamIndex: Int, recurse: (PsiMethod) -> Boolean): Boolean {
    val ref = this.referencedMethod
    if (ref === reference) {
        val param = this.argumentList.expressions[referenceParamIndex]
        if (param is PsiReferenceExpression) {
            val paramRef = param.advancedResolve(false).element
            if (paramRef === method.parameterList.parameters[referenceParamIndex])
                return true
        } else if (param is PsiPolyadicExpression) {
            for (operand in param.operands)
                if (operand is PsiReferenceExpression) {
                    val operandRef = operand.advancedResolve(false).element
                    if (operandRef === method.parameterList.parameters[paramIndex])
                        return true
                }
        }
    } else if (ref != null) {
        return recurse(ref)
    }
    return false
}

fun PsiCall.getSupers(reference: PsiMethod, paramIndex: Int, referenceParamIndex: Int): Iterable<PsiCall> {
    val method = this.resolveMethod() ?: return emptyList()
    if (!method.isConstructor)
        return emptyList()
    if (method === reference)
        return listOf(this)

    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        if (elem is PsiMethodCallExpression &&
            (elem.methodExpression.text == "super" || elem.methodExpression.text == "this"))
            elem
        else
            elem.children.map { findFirstMethodCall(it) }.find { it != null }

    val value = findFirstMethodCall(method)
    if (value is PsiMethodCallExpression) {
        if (value.referencedMethod === reference) {
            val param = value.argumentList.expressions[referenceParamIndex]
            if (param is PsiReferenceExpression) {
                val ref = param.advancedResolve(false).element
                if (ref === method.parameterList.parameters[referenceParamIndex]) {
                    return listOf(this, value)
                }
            } else if (param is PsiPolyadicExpression) {
                for (operand in param.operands) {
                    if (operand is PsiReferenceExpression) {
                        val ref = operand.advancedResolve(false).element
                        if (ref === method.parameterList.parameters[paramIndex]) {
                            return listOf(this, value)
                        }
                    }
                }
            }
        } else {
            val result = mutableListOf(this)
            result.addAll(value.getSupers(reference, paramIndex, referenceParamIndex))
            return result
        }
    }
    return emptyList()
}

fun PsiCall.getCalls(reference: PsiMethod, paramIndex: Int, referenceParamIndex: Int): Iterable<PsiCall> {
    val method = this.resolveMethod() ?: return emptyList()
    if (method === reference)
        return listOf(this)

    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        if (elem is PsiMethodCallExpression)
            elem
        else
            elem.children.map { findFirstMethodCall(it) }.find { it != null }

    val value = findFirstMethodCall(method)
    if (value is PsiMethodCallExpression) {
        if (value.referencedMethod === reference) {
            val param = value.argumentList.expressions[referenceParamIndex]
            if (param is PsiReferenceExpression) {
                val ref = param.advancedResolve(false).element
                if (ref === method.parameterList.parameters[referenceParamIndex])
                    return listOf(this, value)
            } else if (param is PsiPolyadicExpression) {
                for (operand in param.operands)
                    if (operand is PsiReferenceExpression) {
                        val ref = operand.advancedResolve(false).element
                        if (ref === method.parameterList.parameters[paramIndex])
                            return listOf(this, value)
                    }
            }
        } else {
            val result = mutableListOf(this)
            result.addAll(value.getCalls(reference, paramIndex, referenceParamIndex))
            return result
        }
    }
    return emptyList()
}

fun PsiCall.getCallsReturningResult(reference: PsiMethod, paramIndex: Int, referenceParamIndex: Int): Iterable<PsiCall> {
    val method = this.referencedMethod ?: return emptyList()
    if (method.isConstructor)
        return this.getSupers(reference, paramIndex, referenceParamIndex)
    if (!method.returnType!!.isAssignableFrom(reference.returnType!!))
        return emptyList()
    if (method === reference)
        return listOf(this)
    for (returnStatement in PsiUtil.findReturnStatements(method)) {
        val value = returnStatement.returnValue
        if (value is PsiMethodCallExpression) {
            if (value.referencedMethod === reference) {
                val param = value.argumentList.expressions[referenceParamIndex]
                if (param is PsiReferenceExpression) {
                    val ref = param.advancedResolve(false).element
                    if (ref === method.parameterList.parameters[referenceParamIndex]) {
                        return listOf(this, value)
                    }
                } else if (param is PsiPolyadicExpression) {
                    for (operand in param.operands) {
                        if (operand is PsiReferenceExpression) {
                            val ref = operand.advancedResolve(false).element
                            if (ref === method.parameterList.parameters[paramIndex]) {
                                return listOf(this, value)
                            }
                        }
                    }
                } else {
                    if (param.evaluate(null, null) != null) {
                        return listOf(this, value)
                    }
                }
            } else {
                val result = mutableListOf(this)
                result.addAll(value.getCallsReturningResult(reference, paramIndex, referenceParamIndex))
                return result
            }
        }
    }
    return emptyList()
}

fun PsiCall.extractVarArgs(index: Int, substitutions: Map<Int, Array<String?>?>, allowReferences: Boolean, allowTranslations: Boolean): Array<String?> {
    val method = this.referencedMethod
    val args = this.argumentList?.expressions ?: return emptyArray()
    if (method == null || args.size < (index + 1))
        return emptyArray()
    if (!method.parameterList.parameters[index].isVarArgs) {
        return arrayOf(args[index].evaluate(null, null))
    }

    val varargType = method.getSignature(PsiSubstitutor.EMPTY).parameterTypes[index]
    val elements = args.drop(index)
    return extractVarArgs(varargType, elements, substitutions, allowReferences, allowTranslations)
}

fun extractVarArgs(type: PsiType, elements: List<PsiExpression>, substitutions: Map<Int, Array<String?>?>, allowReferences: Boolean, allowTranslations: Boolean): Array<String?> {
    fun convertExpression(expression: PsiExpression): Array<String?>? =
        expression.substituteParameter(substitutions, allowReferences, allowTranslations)

    fun resolveReference(expression: PsiExpression): Array<String?> {
        if (expression is PsiTypeCastExpression && expression.operand != null) {
            return resolveReference(expression.operand!!)
        } else if (expression is PsiReferenceExpression) {
            val reference = expression.advancedResolve(false).element
            if (reference is PsiParameter && reference.parent is PsiParameterList) {
                val paramIndex = (reference.parent as PsiParameterList).getParameterIndex(reference)
                if (substitutions.containsKey(paramIndex)) {
                    return substitutions[paramIndex] ?: arrayOf(null as String?)
                }
            }
            return arrayOf(expression.evaluate(null, null))
        }
        return arrayOf(expression.evaluate(null, null))
    }

    if (elements[0].type == type) {
        // We're dealing with an array initialiser, let's analyse it!
        val initialiser = elements[0]
        if (initialiser is PsiNewExpression && initialiser.arrayInitializer != null)
            return initialiser.arrayInitializer!!.initializers
                .flatMap { convertExpression(it)?.toList() ?: listOf<String?>(null) }
                .toTypedArray()
        else
            return resolveReference(initialiser)
    } else
        return elements
            .flatMap { convertExpression(it)?.toList() ?: listOf<String?>(null) }
            .toTypedArray()
}