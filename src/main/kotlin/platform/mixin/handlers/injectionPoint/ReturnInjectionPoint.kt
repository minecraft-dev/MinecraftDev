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
import com.demonwav.mcdev.util.hasImplicitReturnStatement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.LambdaUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiType
import com.intellij.psi.controlFlow.AnalysisCanceledException
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class AbstractReturnInjectionPoint(private val tailOnly: Boolean) : InjectionPoint<PsiElement>() {
    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor {
        return MyNavigationVisitor(tailOnly)
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<PsiElement> {
        return MyCollectVisitor(at.project, mode, tailOnly)
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>,
    ): LookupElementBuilder? {
        return null
    }

    private class MyNavigationVisitor(private val tailOnly: Boolean) : NavigationVisitor() {
        override fun visitReturnStatement(statement: PsiReturnStatement) {
            if (tailOnly) {
                result.clear()
            }
            addResult(statement)
            super.visitReturnStatement(statement)
        }

        override fun visitEnd(executableElement: PsiElement) {
            val codeBlockToAnalyze = when (executableElement) {
                is PsiMethodReferenceExpression -> {
                    if (tailOnly) {
                        result.clear()
                    }
                    addResult(executableElement)
                    return
                }
                is PsiLambdaExpression -> {
                    val body = executableElement.body ?: return
                    if (body !is PsiCodeBlock) {
                        if (tailOnly) {
                            result.clear()
                        }
                        addResult(body)
                        return
                    }
                    val returnType = LambdaUtil.getFunctionalInterfaceReturnType(executableElement) ?: return
                    if (returnType != PsiType.VOID) {
                        return
                    }
                    body
                }
                is PsiMethod -> {
                    if (executableElement.returnType != PsiType.VOID && !executableElement.isConstructor) {
                        return
                    }
                    executableElement.body ?: return
                }
                is PsiClassInitializer -> {
                    executableElement.body
                }
                else -> return
            }

            val rBrace = codeBlockToAnalyze.rBrace ?: return
            val hasImplicitReturnStatement = try {
                hasImplicitReturnStatement(codeBlockToAnalyze)
            } catch (e: AnalysisCanceledException) {
                return
            }

            if (hasImplicitReturnStatement) {
                if (tailOnly) {
                    result.clear()
                }
                addResult(rBrace)
            }
        }
    }

    private class MyCollectVisitor(
        private val project: Project,
        mode: Mode,
        private val tailOnly: Boolean,
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            fun insnHandler(insn: AbstractInsnNode): Boolean {
                if (insn.opcode !in Opcodes.IRETURN..Opcodes.RETURN) {
                    return false
                }

                val statementText = when (insn.opcode) {
                    Opcodes.RETURN -> "return;"
                    Opcodes.ARETURN -> "return null;"
                    else -> "return 0;"
                }
                val fakeStatement = elementFactory.createStatementFromText(statementText, null)
                    as PsiReturnStatement
                addResult(insn, fakeStatement)
                return true
            }
            if (tailOnly) {
                var insn = insns.last
                while (insn != null) {
                    if (insnHandler(insn)) {
                        break
                    }
                    insn = insn.previous
                }
            } else {
                insns.iterator().forEach(::insnHandler)
            }
        }
    }
}

class ReturnInjectionPoint : AbstractReturnInjectionPoint(false)
class TailInjectionPoint : AbstractReturnInjectionPoint(true)
