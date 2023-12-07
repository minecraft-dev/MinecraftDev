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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.ifNotBlank
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiSwitchLabelStatementBase
import com.intellij.psi.util.PsiUtil
import java.util.Locale
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class ConstantInjectionPoint : InjectionPoint<PsiElement>() {
    fun getConstantInfo(at: PsiAnnotation): ConstantInfo? {
        val args = AtResolver.getArgs(at)
        val nullValue = args["nullValue"]?.let(java.lang.Boolean::parseBoolean) ?: false
        val intValue = args["intValue"]?.toIntOrNull()
        val floatValue = args["floatValue"]?.toFloatOrNull()
        val longValue = args["longValue"]?.toLongOrNull()
        val doubleValue = args["doubleValue"]?.toDoubleOrNull()
        val stringValue = args["stringValue"]
        val classValue = args["classValue"]?.ifNotBlank { Type.getObjectType(it.replace('.', '/')) }
        val count =
            nullValue.toInt() +
                (intValue != null).toInt() +
                (floatValue != null).toInt() +
                (longValue != null).toInt() +
                (doubleValue != null).toInt() +
                (stringValue != null).toInt() +
                (classValue != null).toInt()
        if (count != 1) {
            return null
        }

        val constant = if (nullValue) {
            null
        } else {
            intValue ?: floatValue ?: longValue ?: doubleValue ?: stringValue ?: classValue!!
        }

        val expandConditions = args["expandZeroConditions"]
            ?.replace(" ", "")
            ?.split(',')
            ?.mapNotNull {
                try {
                    ExpandCondition.valueOf(it.uppercase(Locale.ENGLISH))
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            ?.toSet() ?: emptySet()

        return ConstantInfo(constant, expandConditions)
    }

    private fun Boolean.toInt(): Int {
        return if (this) 1 else 0
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor {
        return MyNavigationVisitor(getConstantInfo(at))
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<PsiElement> {
        return MyCollectVisitor(at.project, mode, getConstantInfo(at))
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>,
    ): LookupElementBuilder? {
        return null
    }

    class ConstantInfo(val constant: Any?, val expandConditions: Set<ExpandCondition>)

    enum class ExpandCondition(vararg val opcodes: Int) {
        LESS_THAN_ZERO(Opcodes.IFLT, Opcodes.IFGE),
        LESS_THAN_OR_EQUAL_TO_ZERO(Opcodes.IFLE, Opcodes.IFGT),
        GREATER_THAN_ZERO(Opcodes.IFLE, Opcodes.IFGT),
        GREATER_THAN_OR_EQUAL_TO_ZERO(Opcodes.IFLT, Opcodes.IFGE),
    }

    class MyNavigationVisitor(
        private val constantInfo: ConstantInfo?,
        private val expectedType: Type? = null,
    ) : NavigationVisitor() {
        override fun visitForeachStatement(statement: PsiForeachStatement) {
            if (statement.iteratedValue?.type is PsiArrayType) {
                // index initialized to 0
                visitConstant(statement, 0)
            }
            super.visitForeachStatement(statement)
        }

        override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
            visitConstant(expression, Type.getType(expression.operand.type.descriptor))
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            if (expression.textMatches("null")) {
                visitConstant(expression, null)
            } else {
                super.visitLiteralExpression(expression)
            }
        }

        override fun visitExpression(expression: PsiExpression) {
            if (PsiUtil.isConstantExpression(expression)) {
                val value = expression.constantValue
                if (value != null) {
                    visitConstant(expression, value)
                    return
                }
            }
            super.visitExpression(expression)
        }

        private fun visitConstant(element: PsiElement, value: Any?) {
            if (constantInfo != null && value != constantInfo.constant) {
                return
            }

            if (expectedType != null && value != null) {
                // First check if we expect any String literal
                if (value is String &&
                    (expectedType.sort != Type.OBJECT || expectedType.className != CommonClassNames.JAVA_LANG_STRING)
                ) {
                    return
                }

                // then check if we expect any class literal
                if (value is Type && (
                    expectedType.sort != Type.ARRAY && expectedType.sort != Type.OBJECT ||
                        expectedType.className != CommonClassNames.JAVA_LANG_CLASS
                    )
                ) {
                    return
                }

                // otherwise we expect a primitive literal
                if (expectedType.sort in Type.BOOLEAN..Type.DOUBLE &&
                    value::class.javaPrimitiveType?.let(Type::getType) != expectedType
                ) {
                    return
                }
            }

            val parent = PsiUtil.skipParenthesizedExprUp(element.parent)

            // check for expandZeroConditions
            if (value == null || value == 0) {
                if (parent is PsiBinaryExpression) {
                    val operation = parent.operationTokenType
                    if (operation == JavaTokenType.EQEQ || operation == JavaTokenType.NE) {
                        return
                    }
                    val opcode = when (operation) {
                        JavaTokenType.LT -> Opcodes.IFLT
                        JavaTokenType.LE -> Opcodes.IFLE
                        JavaTokenType.GT -> Opcodes.IFGT
                        JavaTokenType.GE -> Opcodes.IFGE
                        else -> null
                    }
                    if (opcode != null && (
                        constantInfo == null ||
                            !constantInfo.expandConditions.any { opcode in it.opcodes }
                        )
                    ) {
                        return
                    }
                }
            }

            // check for switch statement (compiles to tableswitch or lookupswitch which aren't targeted)
            if (parent is PsiSwitchLabelStatementBase) {
                return
            }

            addResult(element)
        }
    }

    class MyCollectVisitor(
        private val project: Project,
        mode: Mode,
        private val constantInfo: ConstantInfo?,
        private val expectedType: Type? = null,
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) {
            methodNode.instructions?.iterator()?.forEachRemaining { insn ->
                val constant = when (insn) {
                    is InsnNode -> when (insn.opcode) {
                        in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> insn.opcode - Opcodes.ICONST_0
                        Opcodes.LCONST_0 -> 0L
                        Opcodes.LCONST_1 -> 1L
                        Opcodes.FCONST_0 -> 0.0f
                        Opcodes.FCONST_1 -> 1.0f
                        Opcodes.FCONST_2 -> 2.0f
                        Opcodes.DCONST_0 -> 0.0
                        Opcodes.DCONST_1 -> 1.0
                        Opcodes.ACONST_NULL -> null
                        else -> return@forEachRemaining
                    }

                    is IntInsnNode -> when (insn.opcode) {
                        Opcodes.BIPUSH, Opcodes.SIPUSH -> insn.operand
                        else -> return@forEachRemaining
                    }

                    is LdcInsnNode -> insn.cst
                    is JumpInsnNode -> {
                        if (constantInfo == null || !constantInfo.expandConditions.any { insn.opcode in it.opcodes }) {
                            return@forEachRemaining
                        }
                        var lastInsn = insn.previous
                        while (lastInsn != null && (lastInsn is LabelNode || lastInsn is FrameNode)) {
                            lastInsn = lastInsn.previous
                        }
                        if (lastInsn != null) {
                            val lastOpcode = lastInsn.opcode
                            if (lastOpcode == Opcodes.LCMP ||
                                lastOpcode == Opcodes.FCMPL ||
                                lastOpcode == Opcodes.FCMPG ||
                                lastOpcode == Opcodes.DCMPL ||
                                lastOpcode == Opcodes.DCMPG
                            ) {
                                return@forEachRemaining
                            }
                        }
                        0
                    }

                    is TypeInsnNode -> {
                        if (insn.opcode < Opcodes.CHECKCAST) {
                            // Don't treat NEW and ANEWARRAY as constants
                            // Matches Mixin's handling
                            return@forEachRemaining
                        }
                        Type.getObjectType(insn.desc)
                    }

                    else -> return@forEachRemaining
                }

                if (constantInfo != null && constant != constantInfo.constant) {
                    return@forEachRemaining
                }

                if (expectedType != null && constant != null) {
                    // First check if we expect any String literal
                    if (constant is String && (
                        expectedType.sort != Type.OBJECT ||
                            expectedType.className != CommonClassNames.JAVA_LANG_STRING
                        )
                    ) {
                        return@forEachRemaining
                    }

                    // then check if we expect any class literal
                    if (constant is Type && (
                        expectedType.sort != Type.ARRAY && expectedType.sort != Type.OBJECT ||
                            expectedType.className != CommonClassNames.JAVA_LANG_CLASS
                        )
                    ) {
                        return@forEachRemaining
                    }

                    // otherwise we expect a primitive literal
                    if (expectedType.sort in Type.BOOLEAN..Type.DOUBLE &&
                        constant::class.javaPrimitiveType?.let(Type::getType) != expectedType
                    ) {
                        return@forEachRemaining
                    }
                }

                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val literal = if (constant is Type) {
                    elementFactory.createExpressionFromText("${constant.className}.class", null)
                } else {
                    elementFactory.createLiteralExpression(constant)
                }
                addResult(insn, literal)
            }
        }
    }
}
