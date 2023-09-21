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
import java.util.IllegalFormatException

class TranslationFunction(
    private val memberReference: MemberReference,
    val matchedIndex: Int,
    val formatting: Boolean,
    val foldParameters: FoldingScope = FoldingScope.CALL,
    val obfuscatedName: Boolean = false,
) {
    private fun getMethod(context: PsiElement): PsiMethod? {
        var reference = memberReference
        if (obfuscatedName) {
            val moduleSrgManager = context.findMcpModule()?.srgManager
            val srgManager = moduleSrgManager ?: SrgManager.findAnyInstance(context.project)
            srgManager?.srgMapNow?.mapToMcpMethod(memberReference)?.let {
                reference = it
            }
        }
        return reference.resolveMember(context.project) as? PsiMethod
    }

    fun matches(call: PsiCall, paramIndex: Int): Boolean {
        return matches(call.resolveMethod() ?: return false, paramIndex)
    }

    fun matches(method: PsiMethod, paramIndex: Int): Boolean {
        val referenceMethod = getMethod(method) ?: return false
        return method.isSameReference(referenceMethod) && paramIndex == matchedIndex
    }

    fun getTranslationKey(call: PsiCall, param: PsiElement): String? {
        if (!matches(call, matchedIndex)) {
            return null
        }
        return (param as? PsiLiteral)?.value as? String
    }

    fun format(translation: String, call: PsiCall): Pair<String, Int>? {
        if (!matches(call, matchedIndex)) {
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
        return try {
            String.format(format, *varargs) to varargStart
        } catch (e: IllegalFormatException) {
            null
        }
    }

    override fun toString(): String {
        return "$memberReference@$matchedIndex"
    }

    companion object {
        val NUMBER_FORMATTING_PATTERN = Regex("%(\\d+\\$)?[\\d.]*[df]")
        val STRING_FORMATTING_PATTERN = Regex("[^%]?%(?:\\d+\\$)?s")
    }

    enum class FoldingScope {
        CALL,
        PARAMETER,
        PARAMETERS,
    }
}
