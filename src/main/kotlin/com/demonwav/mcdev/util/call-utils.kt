/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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

fun PsiCall.getSupers(reference: PsiMethod, paramIndex: Int, referenceParamIndex: Int): Iterable<PsiCall> {
    val method = this.resolveMethod() ?: return emptyList()
    if (!method.isConstructor) {
        return emptyList()
    }
    if (method.isSameReference(reference) && paramIndex == referenceParamIndex) {
        return listOf(this)
    }

    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        if (
            elem is PsiMethodCallExpression &&
            (elem.methodExpression.text == "super" || elem.methodExpression.text == "this")
        ) {
            elem
        } else {
            elem.children.mapFirstNotNull { findFirstMethodCall(it) }
        }

    val funcThis = this
    val value = findFirstMethodCall(method)
    return value?.extractReferences(this, method, reference, paramIndex, referenceParamIndex, { false }) recurse@{
        if (this === funcThis) {
            return@recurse emptyList()
        }
        return@recurse it.getSupers(reference, paramIndex, referenceParamIndex)
    } ?: emptyList()
}

fun PsiCall.getCalls(reference: PsiMethod, paramIndex: Int, referenceParamIndex: Int): Iterable<PsiCall> {
    val method = this.resolveMethod() ?: return emptyList()
    if (method.isSameReference(reference) && paramIndex == referenceParamIndex) {
        return listOf(this)
    }
    if (paramIndex == -1) {
        return emptyList()
    }

    fun findFirstMethodCall(elem: PsiElement): PsiMethodCallExpression? =
        elem as? PsiMethodCallExpression ?: elem.children.mapFirstNotNull { findFirstMethodCall(it) }

    val funcThis = this
    val value = findFirstMethodCall(method)
    return value?.extractReferences(this, method, reference, paramIndex, referenceParamIndex, { false }) recurse@{
        if (this === funcThis) {
            return@recurse emptyList()
        }
        return@recurse it.getCalls(reference, paramIndex, referenceParamIndex)
    } ?: emptyList()
}

fun PsiCall.getCallsReturningResult(
    reference: PsiMethod,
    paramIndex: Int,
    referenceParamIndex: Int
): Iterable<PsiCall> {
    val method = this.referencedMethod ?: return emptyList()
    if (method.isSameReference(reference) && paramIndex == referenceParamIndex) {
        return listOf(this)
    }
    if (method.isConstructor || reference.isConstructor) {
        return this.getSupers(reference, paramIndex, referenceParamIndex)
    }
    if (!method.returnType!!.isAssignableFrom(reference.returnType!!)) {
        return emptyList()
    }
    val funcThis = this
    return PsiUtil.findReturnStatements(method).asSequence()
        .map { it.returnValue }
        .filterIsInstance<PsiMethodCallExpression>()
        .map { expr ->
            expr.extractReferences(
                this, method, reference, paramIndex, referenceParamIndex,
                {
                    it.evaluate(null, null) != null
                },
                recurse@{
                    if (this === funcThis) {
                        return@recurse emptyList()
                    }
                    return@recurse it.getCallsReturningResult(reference, paramIndex, referenceParamIndex)
                }
            )
        }
        .firstOrNull { it.any() } ?: emptyList()
}

inline fun PsiMethodCallExpression.extractReferences(
    call: PsiCall,
    method: PsiMethod,
    reference: PsiMethod?,
    paramIndex: Int,
    referenceParamIndex: Int,
    defaultParamCase: (PsiExpression) -> Boolean,
    recurse: (PsiMethodCallExpression) -> Iterable<PsiCall>
): Iterable<PsiCall> {
    val ref = this.referencedMethod
    when {
        ref.isSameReference(reference) -> {
            val param = this.argumentList.expressions[referenceParamIndex]
            when (param) {
                is PsiReferenceExpression -> {
                    val paramRef = param.advancedResolve(false).element
                    if (paramRef === method.parameterList.parameters[referenceParamIndex]) {
                        return listOf(call, this)
                    }
                }
                is PsiPolyadicExpression -> {
                    val operandRef = param.operands.asSequence()
                        .filterIsInstance<PsiReferenceExpression>()
                        .filter { method.parameterList.parameters.size > paramIndex }
                        .map { it.advancedResolve(false).element }
                        .find { it === method.parameterList.parameters[paramIndex] }
                    if (operandRef != null) {
                        return listOf(call, this)
                    }
                }
                else -> if (defaultParamCase(param)) {
                    return listOf(call, this)
                }
            }
        }
        ref != null -> {
            val result = recurse(this)
            if (result.any()) {
                return listOf(this) + result
            }
        }
    }
    return emptyList()
}

fun PsiCall.extractVarArgs(
    index: Int,
    substitutions: Map<Int, Array<String?>?>,
    allowReferences: Boolean,
    allowTranslations: Boolean
): Array<String?> {
    val method = this.referencedMethod
    val args = this.argumentList?.expressions ?: return emptyArray()
    if (method == null || args.size < (index + 1)) {
        return emptyArray()
    }
    if (!method.parameterList.parameters[index].isVarArgs) {
        return arrayOf(args[index].evaluate(null, null))
    }

    val varargType = method.getSignature(PsiSubstitutor.EMPTY).parameterTypes[index]
    val elements = args.drop(index)
    return extractVarArgs(varargType, elements, substitutions, allowReferences, allowTranslations)
}

fun extractVarArgs(
    type: PsiType,
    elements: List<PsiExpression>,
    substitutions: Map<Int, Array<String?>?>,
    allowReferences: Boolean,
    allowTranslations: Boolean
): Array<String?> {
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

    return if (elements[0].type == type) {
        // We're dealing with an array initializer, let's analyse it!
        val initializer = elements[0]
        if (initializer is PsiNewExpression && initializer.arrayInitializer != null) {
            initializer.arrayInitializer!!.initializers.asSequence()
                .flatMap { convertExpression(it)?.asSequence() ?: sequenceOf<String?>(null) }
                .toTypedArray()
        } else {
            resolveReference(initializer)
        }
    } else {
        elements.asSequence()
            .flatMap { convertExpression(it)?.asSequence() ?: sequenceOf<String?>(null) }
            .toTypedArray()
    }
}
