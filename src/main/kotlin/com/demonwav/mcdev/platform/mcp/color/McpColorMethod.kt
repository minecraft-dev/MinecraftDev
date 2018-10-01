/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.setColor
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.referencedMethod
import com.demonwav.mcdev.util.runWriteAction
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import java.awt.Color
import java.lang.reflect.Type

data class McpColorMethod(val member: MemberReference, val srgName: Boolean, val params: List<Param>) {
    fun match(call: PsiCall): Boolean {
        val referenced = call.referencedMethod
        return referenced != null && referenced == getMethod(call)
    }

    fun extractColors(call: PsiCall): List<McpColorResult<Color>> {
        return params.mapNotNull { it.extractColor(call) }
    }

    fun validateCall(call: PsiCall): List<McpColorResult<McpColorWarning>> {
        if (!match(call)) {
            return listOf()
        }
        return params.flatMap { it.validateCall(call) }
    }

    private fun getMethod(context: PsiElement): PsiMethod? {
        var reference = member
        if (srgName) {
            val moduleSrgManager = context.findModule()?.let { MinecraftFacet.getInstance(it, McpModuleType)?.srgManager }
            val srgManager = moduleSrgManager ?: SrgManager.findAnyInstance(context.project)
            srgManager?.srgMapNow?.mapToMcpMethod(member)?.let {
                reference = it
            }
        }
        return reference.resolveMember(context.project) as? PsiMethod
    }

    interface Param {
        val description: String
        val hasAlpha: Boolean

        fun extractColor(call: PsiCall): McpColorResult<Color>?

        fun extractColor(result: McpColorResult<Any>): Color?

        fun validateCall(call: PsiCall): List<McpColorResult<McpColorWarning>>

        fun setColor(context: McpColorResult<Color>)
    }

    data class SingleIntParam(val position: Int, override val description: String, override val hasAlpha: Boolean) : Param {
        override fun extractColor(call: PsiCall): McpColorResult<Color>? {
            val args = call.argumentList ?: return null
            val colorArg = args.expressions.getOrNull(position) as? PsiLiteralExpression ?: return null
            val color = extractColor(colorArg) ?: return null

            return McpColorResult(colorArg, this, color)
        }

        override fun extractColor(result: McpColorResult<Any>): Color? {
            return (result.expression as? PsiLiteralExpression)?.let { extractColor(it) }
        }

        private fun extractColor(literal: PsiLiteralExpression): Color? {
            return Color(literal.value as? Int ?: return null, hasAlpha)
        }

        override fun validateCall(call: PsiCall): List<McpColorResult<McpColorWarning>> {
            val args = call.argumentList ?: return emptyList()
            val colorArg = args.expressions.getOrNull(position) as? PsiLiteralExpression ?: return emptyList()
            val literal = colorArg.text

            if (!literal.startsWith("0x")) {
                return listOf(McpColorResult(colorArg, this, McpColorWarning.NoHex))
            }

            if (hasAlpha && literal.length in 7..8) {
                return listOf(McpColorResult(colorArg, this, McpColorWarning.MissingAlpha))
            }

            if (literal.length <= 6) {
                return listOf(McpColorResult(
                    colorArg,
                    this,
                    McpColorWarning.MissingComponents(
                        if (literal.length > 4) listOf("red") else listOf("red", "green")
                    )
                )
                )
            }

            if (!hasAlpha && literal.length >= 9) {
                return listOf(McpColorResult(colorArg, this, McpColorWarning.SuperfluousAlpha))
            }

            return emptyList()
        }

        override fun setColor(context: McpColorResult<Color>) {
            val literal = context.expression as? PsiLiteralExpression ?: return
            literal.setColor(context.arg.rgb, hasAlpha)
        }

        object Deserializer : JsonDeserializer<SingleIntParam> {
            override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): SingleIntParam {
                val obj = json.asJsonObject
                return SingleIntParam(
                    obj["position"]?.asInt ?: 0,
                    obj["description"]?.asString ?: "Color",
                    obj["hasAlpha"]?.asBoolean ?: true
                )
            }
        }
    }

    data class FloatVectorParam(val startPosition: Int, override val description: String, override val hasAlpha: Boolean) : Param {
        val length = if (hasAlpha) 4 else 3
        val endIndexExclusive = startPosition + length

        override fun extractColor(call: PsiCall): McpColorResult<Color>? {
            if (validateCall(call).isNotEmpty()) {
                return null
            }

            val args = call.argumentList ?: return null
            val colorArgs = args.expressions.toList().subList(startPosition, endIndexExclusive)
            val components = colorArgs.mapNotNull { evaluate(it) }
            if (components.size < length) {
                return null
            }
            val r = components[0]
            val g = components[1]
            val b = components[2]
            val a = components.getOrNull(3) ?: 1f

            return McpColorResult(call, this, Color(r, g, b, a), colorArgs[0].textRange.union(colorArgs[length - 1].textRange))
        }

        private fun evaluate(element: PsiElement): Float? {
            val facade = JavaPsiFacade.getInstance(element.project)
            return facade.constantEvaluationHelper.computeConstantExpression(element) as? Float
        }

        override fun extractColor(result: McpColorResult<Any>): Color? {
            val call = result.expression as? PsiCall ?: return null
            return extractColor(call)?.arg
        }

        override fun validateCall(call: PsiCall): List<McpColorResult<McpColorWarning>> {
            val args = call.argumentList ?: return emptyList()
            val colorArgs = args.expressions.toList().subList(startPosition, endIndexExclusive)
            val components = colorArgs.mapNotNull(::evaluate)
            if (components.size < length) {
                return emptyList()
            }

            val outOfRange = components.withIndex()
                .filter { it.value !in 0f..1f }
                .map {
                    McpColorResult(
                        colorArgs[it.index],
                        this,
                        McpColorWarning.ComponentOutOfRange("0.0f", "1.0f") { _ ->
                            val literal = colorArgs[it.index]
                            literal.containingFile.runWriteAction {
                                val node = literal.node

                                val literalExpression = JavaPsiFacade.getElementFactory(literal.project)
                                    .createExpressionFromText(it.value.coerceIn(0f, 1f).format(), null) as PsiLiteralExpression

                                node.psi.replace(literalExpression)
                            }
                        }
                    )
                }.toList()

            return outOfRange
        }

        override fun setColor(context: McpColorResult<Color>) {
            val call = context.expression as? PsiCall ?: return
            val expressions = call.argumentList ?: return

            val color = context.arg
            val components = arrayOf(color.red, color.green, color.blue, color.alpha)

            expressions.containingFile.runWriteAction {
                val facade = JavaPsiFacade.getElementFactory(expressions.project)
                for (i in 0 until length) {
                    val expression = expressions.expressions[startPosition + i]
                    val node = expression.node
                    val value = if (expression is PsiLiteralExpression) (components[i] / 255f).format() else "${components[i]} / 255f"
                    val newExpression = facade.createExpressionFromText(value, null)

                    node.psi.replace(newExpression)
                }
            }
        }

        object Deserializer : JsonDeserializer<FloatVectorParam> {
            override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): FloatVectorParam {
                val obj = json.asJsonObject
                return FloatVectorParam(
                    obj["startPosition"]?.asInt ?: 0,
                    obj["description"]?.asString ?: "Color",
                    obj["hasAlpha"]?.asBoolean ?: true
                )
            }
        }
    }

    data class IntVectorParam(val startIndex: Int, override val description: String, override val hasAlpha: Boolean) : Param {
        val length = if (hasAlpha) 4 else 3
        val endIndexExclusive = startIndex + length

        override fun extractColor(call: PsiCall): McpColorResult<Color>? {
            if (validateCall(call).isNotEmpty()) {
                return null
            }

            val args = call.argumentList ?: return null
            val colorArgs = args.expressions.toList().subList(startIndex, endIndexExclusive)
            val components = colorArgs.mapNotNull { evaluate(it) }
            if (components.size < length) {
                return null
            }
            val r = components[0]
            val g = components[1]
            val b = components[2]
            val a = components.getOrNull(3) ?: 255

            return McpColorResult(call, this, Color(r, g, b, a), colorArgs[0].textRange.union(colorArgs[length - 1].textRange))
        }

        private fun evaluate(element: PsiElement): Int? {
            val facade = JavaPsiFacade.getInstance(element.project)
            return facade.constantEvaluationHelper.computeConstantExpression(element) as? Int
        }

        override fun extractColor(result: McpColorResult<Any>): Color? {
            val call = result.expression as? PsiCall ?: return null
            return extractColor(call)?.arg
        }

        override fun validateCall(call: PsiCall): List<McpColorResult<McpColorWarning>> {
            val args = call.argumentList ?: return emptyList()
            val colorArgs = args.expressions.toList().subList(startIndex, endIndexExclusive)
            val components = colorArgs.mapNotNull(::evaluate)
            if (components.size < length) {
                return emptyList()
            }

            val outOfRange = components.withIndex()
                .filter { it.value !in 0..255 }
                .map {
                    McpColorResult(
                        colorArgs[it.index],
                        this,
                        McpColorWarning.ComponentOutOfRange("0", "255") { _ ->
                            val literal = colorArgs[it.index]
                            literal.containingFile.runWriteAction {
                                val node = literal.node

                                val literalExpression = JavaPsiFacade.getElementFactory(literal.project)
                                    .createExpressionFromText(it.value.coerceIn(0, 255).toString(), null) as PsiLiteralExpression

                                node.psi.replace(literalExpression)
                            }
                        }
                    )
                }.toList()

            return outOfRange
        }

        override fun setColor(context: McpColorResult<Color>) {
            val call = context.expression as? PsiCall ?: return
            val expressions = call.argumentList ?: return

            val color = context.arg
            val components = arrayOf(color.red, color.green, color.blue, color.alpha)

            expressions.containingFile.runWriteAction {
                val facade = JavaPsiFacade.getElementFactory(expressions.project)
                for (i in 0 until length) {
                    val expression = expressions.expressions[startIndex + i]
                    val node = expression.node
                    val newExpression = facade.createExpressionFromText(components[i].toString(), null)

                    node.psi.replace(newExpression)
                }
            }
        }

        object Deserializer : JsonDeserializer<IntVectorParam> {
            override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): IntVectorParam {
                val obj = json.asJsonObject
                return IntVectorParam(
                    obj["startPosition"]?.asInt ?: 0,
                    obj["description"]?.asString ?: "Color",
                    obj["hasAlpha"]?.asBoolean ?: true
                )
            }
        }
    }
}

private fun Float.format(): String {
    val number = if (this == 0f || this == 1f) this.toInt().toString() else this.toString()
    return "${number}f"
}
