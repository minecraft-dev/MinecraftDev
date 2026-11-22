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

package com.demonwav.mcdev.platform.minecraft.unpick.eval

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.DataType
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickBitNotExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickCastExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupConstant
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickLiteralExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickNegateExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickParenExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickBinaryExpressionMixin
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.util.elementType

object UnpickExpressionEvaluator {

    /**
     * A value produced by a group constant. If the expression contained a wildcard, [wildcardField] is the field the
     * wildcard was expanded to.
     */
    class EvaluatedConstant(val constant: UnpickGroupConstant, val value: UnpickValue, val wildcardField: PsiField?)

    class Result(val values: List<EvaluatedConstant>, val problems: Set<UnpickProblem>)

    fun evaluate(constant: UnpickGroupConstant): Result {
        return CachedValuesManager.getCachedValue(constant) {
            CachedValueProvider.Result.create(doEvaluate(constant), PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    private fun doEvaluate(constant: UnpickGroupConstant): Result {
        val expression = constant.expression
        val groupType = constant.groupDefinition?.dataType?.dataType?.takeIf { it.isValidGroupType }
        val wildcards = PsiTreeUtil.findChildrenOfType(constant, UnpickFieldExpression::class.java)
            .filter { it.isWildcard }
        if (wildcards.size > 1) {
            // Reported by the annotator
            return Result(emptyList(), emptySet())
        }

        val problems = LinkedHashSet<UnpickProblem>()
        val values = mutableListOf<EvaluatedConstant>()

        val wildcard = wildcards.singleOrNull()
        if (wildcard == null) {
            Evaluation(groupType, null, problems).evaluate(expression)?.let { value ->
                values += EvaluatedConstant(constant, value, null)
                checkGroupType(expression, value, groupType, problems)
            }
        } else {
            if (wildcard.className.resolveClass() == null) {
                return Result(emptyList(), emptySet())
            }
            val fields = findWildcardFields(wildcard, groupType)
            if (fields.isEmpty()) {
                problems += UnpickProblem.Field(
                    wildcard.wildcardElement ?: wildcard,
                    MCDevBundle("minecraft.unpick.eval.wildcard_no_match", wildcard.className.qualifiedName),
                )
            }
            for (field in fields) {
                val value = Evaluation(groupType, field, problems).evaluate(expression) ?: continue
                values += EvaluatedConstant(constant, value, field)
                checkGroupType(expression, value, groupType, problems)
            }
        }

        return Result(values, problems)
    }

    private fun checkGroupType(
        expression: UnpickExpression,
        value: UnpickValue,
        groupType: DataType?,
        problems: MutableSet<UnpickProblem>,
    ) {
        if (groupType == null || value.type.canWidenTo(groupType)) {
            return
        }
        problems += UnpickProblem.Type(
            expression,
            MCDevBundle("minecraft.unpick.eval.incompatible_with_group", value.type.keyword, groupType.keyword),
        )
    }

    private fun findWildcardFields(expression: UnpickFieldExpression, groupType: DataType?): List<PsiField> {
        val psiClass = expression.className.resolveClass() ?: return emptyList()
        val explicitType = expression.explicitDataType?.dataType
        val isStatic = !expression.isInstance
        return psiClass.fields.filter { field ->
            if (field.hasModifierProperty(PsiModifier.STATIC) != isStatic) {
                return@filter false
            }
            val fieldType = DataType.fromPsiType(field.type) ?: return@filter false
            val typeMatches = if (explicitType != null) {
                fieldType == explicitType
            } else {
                groupType != null && fieldType.canWidenTo(groupType)
            }
            typeMatches && constantValue(field) != null
        }
    }

    /**
     * Returns the value of a constant field, as defined by the unpick spec.
     *
     * If the field is compiled and has no source, `null` and `Class` constants can't be distinguished from
     * non-constant fields, so an unknown value is returned for them.
     */
    fun constantValue(field: PsiField): UnpickValue? = constantValue(field, 0)

    private fun constantValue(field: PsiField, depth: Int): UnpickValue? {
        val type = DataType.fromPsiType(field.type) ?: return null
        if (!field.hasModifierProperty(PsiModifier.FINAL)) {
            return null
        }
        if (type.isPrimitive) {
            // Java's implementations return a value of the field's type, but this isn't guaranteed for all languages
            val value = TypeConversionUtil.computeCastTo(field.computeConstantValue(), field.type) ?: return null
            return UnpickValue(type, value)
        }
        if (type == DataType.STRING) {
            (field.computeConstantValue() as? String)?.let { return UnpickValue(DataType.STRING, it) }
        } else {
            primitiveClassConstant(field)?.let { return UnpickValue(DataType.CLASS, it) }
        }

        val sourceField = field.navigationElement as? PsiField ?: field
        if (sourceField is PsiCompiledElement) {
            return UnpickValue.unknown(type)
        }
        return when (val initializer = PsiUtil.skipParenthesizedExprDown(sourceField.initializer)) {
            is PsiLiteralExpression -> if (initializer.firstChild.elementType == JavaTokenType.NULL_KEYWORD) {
                UnpickValue(type, null)
            } else {
                null
            }
            is PsiClassObjectAccessExpression -> if (type == DataType.CLASS) {
                UnpickValue(DataType.CLASS, initializer.operand.type)
            } else {
                null
            }
            is PsiReferenceExpression -> {
                val referenced = initializer.resolve() as? PsiField
                if (referenced != null && depth < MAX_FIELD_DEPTH && DataType.fromPsiType(referenced.type) == type) {
                    constantValue(referenced, depth + 1)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * The `TYPE` fields of the boxed types are the class objects of the primitive types.
     */
    private fun primitiveClassConstant(field: PsiField): PsiType? {
        val containingClass = field.containingClass ?: return null
        if (field.name != "TYPE") {
            return null
        }
        if (containingClass.qualifiedName == CommonClassNames.JAVA_LANG_VOID) {
            return PsiTypes.voidType()
        }
        val boxedType = JavaPsiFacade.getElementFactory(field.project).createType(containingClass)
        return PsiPrimitiveType.getUnboxedType(boxedType)
    }

    private const val MAX_FIELD_DEPTH = 8

    private class Evaluation(
        private val groupType: DataType?,
        private val wildcardField: PsiField?,
        private val problems: MutableSet<UnpickProblem>,
    ) {
        fun evaluate(expression: UnpickExpression?): UnpickValue? {
            return when (expression) {
                null -> null
                is UnpickLiteralExpression -> {
                    val type = expression.dataType ?: return null
                    val value = expression.value ?: return null
                    UnpickValue(type, value)
                }
                is UnpickParenExpression -> evaluate(expression.operand)
                is UnpickNegateExpression -> evaluateNegate(expression)
                is UnpickBitNotExpression -> evaluateBitNot(expression)
                is UnpickCastExpression -> evaluateCast(expression)
                is UnpickFieldExpression -> evaluateField(expression)
                is UnpickBinaryExpressionMixin -> evaluateBinary(expression)
                else -> null
            }
        }

        private fun typeProblem(element: PsiElement, key: String, vararg params: Any) {
            problems += UnpickProblem.Type(element, MCDevBundle(key, *params))
        }

        private fun fieldProblem(element: PsiElement, key: String, vararg params: Any) {
            problems += UnpickProblem.Field(element, MCDevBundle(key, *params))
        }

        private fun evaluateNegate(expression: UnpickNegateExpression): UnpickValue? {
            val operand = evaluate(expression.operand) ?: return null
            if (!operand.type.isPrimitive) {
                typeProblem(expression, "minecraft.unpick.eval.bad_unary_operand", "-", operand.type.keyword)
                return null
            }
            val promoted = operand.convertTo(unaryPromote(operand.type))
            if (!promoted.isKnown) {
                return promoted
            }
            return when (val value = promoted.value) {
                is Int -> UnpickValue(DataType.INT, -value)
                is Long -> UnpickValue(DataType.LONG, -value)
                is Float -> UnpickValue(DataType.FLOAT, -value)
                is Double -> UnpickValue(DataType.DOUBLE, -value)
                else -> null
            }
        }

        private fun evaluateBitNot(expression: UnpickBitNotExpression): UnpickValue? {
            val operand = evaluate(expression.operand) ?: return null
            if (!operand.type.isIntegral) {
                typeProblem(expression, "minecraft.unpick.eval.bad_unary_operand", "~", operand.type.keyword)
                return null
            }
            val promoted = operand.convertTo(unaryPromote(operand.type))
            if (!promoted.isKnown) {
                return promoted
            }
            return when (val value = promoted.value) {
                is Int -> UnpickValue(DataType.INT, value.inv())
                is Long -> UnpickValue(DataType.LONG, value.inv())
                else -> null
            }
        }

        private fun evaluateCast(expression: UnpickCastExpression): UnpickValue? {
            val operand = evaluate(expression.operand)
            val target = expression.dataType.dataType ?: return null
            if (operand == null) {
                return null
            }
            if (!operand.canConvertTo(target)) {
                typeProblem(expression, "minecraft.unpick.eval.bad_cast", operand.type.keyword, target.keyword)
                return null
            }
            return operand.convertTo(target)
        }

        private fun evaluateBinary(expression: UnpickBinaryExpressionMixin): UnpickValue? {
            val left = evaluate(expression.left)
            val right = evaluate(expression.right)
            if (left == null || right == null) {
                return null
            }
            val operator = expression.operator
            val operatorText = expression.operatorToken.text

            if (operator == UnpickTypes.PLUS && (left.type == DataType.STRING || right.type == DataType.STRING)) {
                val leftString = left.toJavaString()
                val rightString = right.toJavaString()
                if (leftString == null || rightString == null) {
                    return UnpickValue.unknown(DataType.STRING)
                }
                return UnpickValue(DataType.STRING, leftString + rightString)
            }

            val isArithmetic = operator == UnpickTypes.PLUS || operator == UnpickTypes.MINUS ||
                operator == UnpickTypes.STAR || operator == UnpickTypes.SLASH || operator == UnpickTypes.PERCENT
            val isShift = operator == UnpickTypes.SHL || operator == UnpickTypes.SHR || operator == UnpickTypes.USHR
            val valid = if (isArithmetic) {
                left.type.isPrimitive && right.type.isPrimitive
            } else {
                left.type.isIntegral && right.type.isIntegral
            }
            if (!valid) {
                typeProblem(
                    expression,
                    "minecraft.unpick.eval.bad_binary_operands",
                    operatorText,
                    left.type.keyword,
                    right.type.keyword,
                )
                return null
            }

            if (isShift) {
                val resultType = unaryPromote(left.type)
                if (!left.isKnown || !right.isKnown) {
                    return UnpickValue.unknown(resultType)
                }
                val distance = right.asLong().toInt()
                return when (val value = left.convertTo(resultType).value) {
                    is Int -> UnpickValue(
                        DataType.INT,
                        when (operator) {
                            UnpickTypes.SHL -> value shl distance
                            UnpickTypes.SHR -> value shr distance
                            else -> value ushr distance
                        },
                    )
                    is Long -> UnpickValue(
                        DataType.LONG,
                        when (operator) {
                            UnpickTypes.SHL -> value shl distance
                            UnpickTypes.SHR -> value shr distance
                            else -> value ushr distance
                        },
                    )
                    else -> null
                }
            }

            val resultType = binaryPromote(left.type, right.type)
            if (!left.isKnown || !right.isKnown) {
                return UnpickValue.unknown(resultType)
            }
            val l = left.convertTo(resultType).value
            val r = right.convertTo(resultType).value

            if ((operator == UnpickTypes.SLASH || operator == UnpickTypes.PERCENT) && resultType.isIntegral &&
                right.asLong() == 0L
            ) {
                problems += UnpickProblem.DivisionByZero(
                    expression.right ?: expression,
                    MCDevBundle("minecraft.unpick.eval.division_by_zero"),
                )
                return UnpickValue.unknown(resultType)
            }

            val result: Any = when (operator) {
                UnpickTypes.PLUS -> arithmetic(l, r, Int::plus, Long::plus, Float::plus, Double::plus)
                UnpickTypes.MINUS -> arithmetic(l, r, Int::minus, Long::minus, Float::minus, Double::minus)
                UnpickTypes.STAR -> arithmetic(l, r, Int::times, Long::times, Float::times, Double::times)
                UnpickTypes.SLASH -> arithmetic(l, r, Int::div, Long::div, Float::div, Double::div)
                UnpickTypes.PERCENT -> arithmetic(l, r, Int::rem, Long::rem, Float::rem, Double::rem)
                UnpickTypes.AMPERSAND -> bitwise(l, r, Int::and, Long::and)
                UnpickTypes.PIPE -> bitwise(l, r, Int::or, Long::or)
                UnpickTypes.CARET -> bitwise(l, r, Int::xor, Long::xor)
                else -> return null
            }
            return UnpickValue(resultType, result)
        }

        private inline fun arithmetic(
            l: Any?,
            r: Any?,
            intOp: (Int, Int) -> Int,
            longOp: (Long, Long) -> Long,
            floatOp: (Float, Float) -> Float,
            doubleOp: (Double, Double) -> Double,
        ): Any {
            return when (l) {
                is Int -> intOp(l, r as Int)
                is Long -> longOp(l, r as Long)
                is Float -> floatOp(l, r as Float)
                else -> doubleOp(l as Double, r as Double)
            }
        }
        private inline fun bitwise(l: Any?, r: Any?, intOp: (Int, Int) -> Int, longOp: (Long, Long) -> Long): Any {
            return when (l) {
                is Int -> intOp(l, r as Int)
                else -> longOp(l as Long, r as Long)
            }
        }

        private fun evaluateField(expression: UnpickFieldExpression): UnpickValue? {
            val field = if (expression.isWildcard) {
                wildcardField
            } else {
                expression.fieldName?.resolveField()
            } ?: return null
            val nameElement = expression.fieldName ?: expression.wildcardElement ?: expression
            val name = field.name

            val isStatic = field.hasModifierProperty(PsiModifier.STATIC)
            if (isStatic && expression.isInstance) {
                fieldProblem(nameElement, "minecraft.unpick.eval.field_is_static", name)
            } else if (!isStatic && !expression.isInstance) {
                fieldProblem(nameElement, "minecraft.unpick.eval.field_not_static", name)
            }
            val fieldType = DataType.fromPsiType(field.type)
            if (fieldType == null) {
                fieldProblem(
                    nameElement,
                    "minecraft.unpick.eval.field_unsupported_type",
                    name,
                    field.type.presentableText,
                )
                return null
            }
            if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                fieldProblem(nameElement, "minecraft.unpick.eval.field_not_final", name)
                return UnpickValue.unknown(fieldType)
            }
            val explicitType = expression.explicitDataType?.dataType
            if (explicitType != null && explicitType != fieldType) {
                fieldProblem(
                    expression.explicitDataType ?: nameElement,
                    "minecraft.unpick.eval.field_type_mismatch",
                    name,
                    fieldType.keyword,
                    explicitType.keyword,
                )
            }

            val value = constantValue(field)
            if (value == null) {
                fieldProblem(nameElement, "minecraft.unpick.eval.field_not_constant", name)
                return UnpickValue.unknown(fieldType)
            }
            // A wildcard without an explicit data type has the data type of the group
            if (expression.isWildcard && explicitType == null && groupType != null && value.canConvertTo(groupType)) {
                return value.convertTo(groupType)
            }
            return value
        }

        private fun unaryPromote(type: DataType): DataType {
            return when (type) {
                DataType.BYTE, DataType.SHORT, DataType.CHAR -> DataType.INT
                else -> type
            }
        }

        private fun binaryPromote(a: DataType, b: DataType): DataType {
            return when {
                a == DataType.DOUBLE || b == DataType.DOUBLE -> DataType.DOUBLE
                a == DataType.FLOAT || b == DataType.FLOAT -> DataType.FLOAT
                a == DataType.LONG || b == DataType.LONG -> DataType.LONG
                else -> DataType.INT
            }
        }
    }
}
