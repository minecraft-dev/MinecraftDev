/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.handlers.ModifyVariableInfo
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.AsmDfaUtil
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiUnaryExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

abstract class AbstractLoadInjectionPoint(private val store: Boolean) : InjectionPoint<PsiElement>() {
    private fun getModifyVariableInfo(at: PsiAnnotation, mode: CollectVisitor.Mode?): ModifyVariableInfo? {
        val modifyVariable = at.parentOfType<PsiAnnotation>() ?: return null
        if (!modifyVariable.hasQualifiedName(MODIFY_VARIABLE)) {
            return null
        }
        return ModifyVariableInfo.getModifyVariableInfo(modifyVariable, mode)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        val info = getModifyVariableInfo(at, null) ?: return null
        return MyNavigationVisitor(info, store)
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement>? {
        val module = at.findModule() ?: return null
        val info = getModifyVariableInfo(at, mode) ?: return null
        return MyCollectVisitor(module, targetClass, mode, info, store)
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>
    ): LookupElementBuilder? {
        return null
    }

    override fun addOrdinalFilter(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
        val ordinal = at.findDeclaredAttributeValue("ordinal")?.constantValue as? Int ?: return
        if (ordinal < 0) return

        // Replace the ordinal filter with one that takes into account the type of the local variable being modified.
        // Fixes otherwise incorrect results for completion.
        val project = at.project
        val ordinals = mutableMapOf<String, Int>()
        collectVisitor.addResultFilter("ordinal") { result, method ->
            result.originalInsn as? VarInsnNode
                ?: throw IllegalStateException("AbstractLoadInjectionPoint returned non-var insn")
            val localInsn = if (store) { result.originalInsn.next } else { result.originalInsn }
            val localType = AsmDfaUtil.getLocalVariableType(
                project,
                targetClass,
                method,
                localInsn,
                result.originalInsn.`var`
            ) ?: return@addResultFilter true
            val desc = localType.descriptor
            val ord = ordinals[desc] ?: 0
            ordinals[desc] = ord + 1
            ord == ordinal
        }
    }

    private class MyNavigationVisitor(
        private val info: ModifyVariableInfo,
        private val store: Boolean
    ) : NavigationVisitor() {
        override fun visitThisExpression(expression: PsiThisExpression) {
            super.visitThisExpression(expression)
            if (!store && expression.qualifier == null) {
                addLocalUsage(expression, "this")
            }
        }

        override fun visitVariable(variable: PsiVariable) {
            super.visitVariable(variable)
            if (store && variable.initializer != null) {
                val name = variable.name
                if (name != null) {
                    addLocalUsage(variable, name)
                }
            }
        }

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            super.visitReferenceExpression(expression)
            val referenceName = expression.referenceName ?: return
            if (expression.qualifierExpression == null) {
                val isCorrectAccessType = if (store) {
                    PsiUtil.isAccessedForWriting(expression)
                } else {
                    PsiUtil.isAccessedForReading(expression)
                }
                if (!isCorrectAccessType) {
                    return
                }
                val resolved = expression.resolve() as? PsiVariable ?: return
                val type = resolved.type
                if (type is PsiPrimitiveType &&
                    type != PsiType.FLOAT &&
                    type != PsiType.DOUBLE &&
                    type != PsiType.LONG &&
                    type != PsiType.BOOLEAN
                ) {
                    // ModifyVariable currently cannot handle iinc
                    val parentExpr = PsiUtil.skipParenthesizedExprUp(expression.parent)
                    val isIincUnary = parentExpr is PsiUnaryExpression &&
                        (
                            parentExpr.operationSign.tokenType == JavaTokenType.PLUSPLUS ||
                                parentExpr.operationSign.tokenType == JavaTokenType.MINUSMINUS
                            )
                    val isIincAssignment = parentExpr is PsiAssignmentExpression &&
                        (
                            parentExpr.operationSign.tokenType == JavaTokenType.PLUSEQ ||
                                parentExpr.operationSign.tokenType == JavaTokenType.MINUSEQ
                            ) &&
                        PsiUtil.isConstantExpression(parentExpr.rExpression) &&
                        (parentExpr.rExpression?.constantValue as? Number)?.toInt()
                        ?.let { it >= Short.MIN_VALUE && it <= Short.MAX_VALUE } == true
                    val isIinc = isIincUnary || isIincAssignment
                    if (isIinc) {
                        if (store) {
                            return
                        }
                        val parentParent = PsiUtil.skipParenthesizedExprUp(parentExpr.parent)
                        if (parentParent is PsiExpressionStatement) {
                            return
                        }
                    }
                }
                if (!info.argsOnly || resolved is PsiParameter) {
                    addLocalUsage(expression, referenceName)
                }
            }
        }

        override fun visitForeachStatement(statement: PsiForeachStatement) {
            checkImplicitLocalsPre(statement)
            if (store) {
                addLocalUsage(statement.iterationParameter, statement.iterationParameter.name)
            }
            super.visitForeachStatement(statement)
            checkImplicitLocalsPost(statement)
        }

        private fun checkImplicitLocalsPre(location: PsiElement) {
            val localsHere = LocalVariables.guessLocalsAt(location, info.argsOnly, true)
            val localIndex = LocalVariables.guessLocalVariableIndex(location) ?: return
            val localCount = LocalVariables.getLocalVariableSize(location)
            for (i in localIndex until (localIndex + localCount)) {
                val local = localsHere.firstOrNull { it.index == i } ?: continue
                if (store) {
                    repeat(local.implicitStoreCountBefore) {
                        addLocalUsage(location, local.name, localsHere)
                    }
                } else {
                    repeat(local.implicitLoadCountBefore) {
                        addLocalUsage(location, local.name, localsHere)
                    }
                }
            }
        }

        private fun checkImplicitLocalsPost(location: PsiElement) {
            val localsHere = LocalVariables.guessLocalsAt(location, info.argsOnly, false)
            val localIndex = LocalVariables.guessLocalVariableIndex(location) ?: return
            val localCount = LocalVariables.getLocalVariableSize(location)
            for (i in localIndex until (localIndex + localCount)) {
                val local = localsHere.firstOrNull { it.index == i } ?: continue
                if (store) {
                    repeat(local.implicitStoreCountAfter) {
                        addLocalUsage(location, local.name, localsHere)
                    }
                } else {
                    repeat(local.implicitLoadCountAfter) {
                        addLocalUsage(location, local.name, localsHere)
                    }
                }
            }
        }

        private fun addLocalUsage(location: PsiElement, name: String) {
            val localsHere = LocalVariables.guessLocalsAt(location, info.argsOnly, !store)
            addLocalUsage(location, name, localsHere)
        }

        private fun addLocalUsage(
            location: PsiElement,
            name: String,
            localsHere: List<LocalVariables.SourceLocalVariable>
        ) {
            if (info.ordinal != null) {
                val local = localsHere.asSequence().filter {
                    it.type.isErasureEquivalentTo(info.type)
                }.drop(info.ordinal).firstOrNull()
                if (name == local?.name) {
                    addResult(location)
                }
                return
            }

            if (info.index != null) {
                val local = localsHere.getOrNull(info.index)
                if (name == local?.name) {
                    addResult(location)
                }
                return
            }

            if (info.names.isNotEmpty()) {
                val matchingLocals = localsHere.filter {
                    info.names.contains(it.mixinName)
                }
                for (local in matchingLocals) {
                    if (local.name == name) {
                        addResult(location)
                    }
                }
                return
            }

            // implicit mode
            val local = localsHere.singleOrNull {
                it.type.isErasureEquivalentTo(info.type)
            }
            if (local != null && local.name == name) {
                addResult(location)
            }
        }
    }

    private class MyCollectVisitor(
        private val module: Module,
        private val targetClass: ClassNode,
        mode: Mode,
        private val info: ModifyVariableInfo,
        private val store: Boolean
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) {
            var opcode = when (info.type) {
                null -> null
                !is PsiPrimitiveType -> Opcodes.ALOAD
                PsiType.LONG -> Opcodes.LLOAD
                PsiType.FLOAT -> Opcodes.FLOAD
                PsiType.DOUBLE -> Opcodes.DLOAD
                else -> Opcodes.ILOAD
            }
            if (store && opcode != null) {
                opcode += (Opcodes.ISTORE - Opcodes.ILOAD)
            }
            for (insn in methodNode.instructions) {
                if (insn !is VarInsnNode) {
                    continue
                }
                if (opcode != null) {
                    if (opcode != insn.opcode) {
                        continue
                    }
                } else {
                    if (store) {
                        if (insn.opcode < Opcodes.ISTORE || insn.opcode > Opcodes.ASTORE) {
                            continue
                        }
                    } else {
                        if (insn.opcode < Opcodes.ILOAD || insn.opcode > Opcodes.ALOAD) {
                            continue
                        }
                    }
                }

                val localLocation = if (store) insn.next ?: insn else insn
                val locals = info.getLocals(module, targetClass, methodNode, localLocation) ?: continue

                val elementFactory = JavaPsiFacade.getElementFactory(module.project)

                for (result in info.matchLocals(locals, mode)) {
                    addResult(insn, elementFactory.createExpressionFromText(result.name, null))
                }
            }
        }
    }
}

class LoadInjectionPoint : AbstractLoadInjectionPoint(false)
class StoreInjectionPoint : AbstractLoadInjectionPoint(true)
