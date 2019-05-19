/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.extractVarArgs
import com.demonwav.mcdev.util.findMcpModule
import com.demonwav.mcdev.util.isSameReference
import com.demonwav.mcdev.util.referencedMethod
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod

data class TranslationFunction(
    val member: MemberReference,
    val srgName: Boolean,
    val paramIndex: Int,
    val keyPrefix: String,
    val keySuffix: String,
    val formatting: Boolean,
    val foldParametersOnly: Boolean
) {
    private fun getMethod(context: PsiElement): PsiMethod? {
        var reference = member
        if (srgName) {
            val moduleSrgManager = context.findMcpModule()?.srgManager
            val srgManager = moduleSrgManager ?: SrgManager.findAnyInstance(context.project)
            srgManager?.srgMapNow?.mapToMcpMethod(member)?.let {
                reference = it
            }
        }
        return reference.resolveMember(context.project) as? PsiMethod
    }

    fun matches(call: PsiCall, paramIndex: Int): Boolean {
        val referenceMethod = getMethod(call) ?: return false
        val method = call.resolveMethod() ?: return false
        return method.isSameReference(referenceMethod) && paramIndex == this.paramIndex
    }

    fun getTranslationKey(call: PsiCall, param: PsiElement): TranslationInstance.Key? {
        if (!matches(call, paramIndex)) {
            return null
        }
        val value = ((param as? PsiLiteral)?.value as? String) ?: return null
        return TranslationInstance.Key(keyPrefix, value, keySuffix)
    }

    fun format(translation: String, call: PsiCall): Pair<String, Int>? {
        if (!matches(call, paramIndex)) {
            return null
        }
        if (!formatting) {
            return translation to -1
        }

        val format = NUMBER_FORMATTING_PATTERN.replace(translation, "%$1s")
        val paramCount = STRING_FORMATTING_PATTERN.findAll(format).count()

        val method = call.referencedMethod ?: return translation to -1
        val varargs = call.extractVarArgs(method.parameterList.parametersCount - 1, true, true)
        val varargStart = if (varargs.size > paramCount) method.parameterList.parametersCount - 1 + paramCount else -1
        return String.format(format, *varargs) to varargStart
    }

    override fun toString(): String {
        return "$member@$paramIndex"
    }

    companion object {
        val NUMBER_FORMATTING_PATTERN = Regex("%(\\d+\\$)?[\\d.]*[df]")
        val STRING_FORMATTING_PATTERN = Regex("[^%]?%(?:\\d+\\$)?s")
    }
}
