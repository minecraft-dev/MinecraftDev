/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReturnStatement
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class AbstractReturnInjectionPoint(private val tailOnly: Boolean) : InjectionPoint<PsiReturnStatement>() {
    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor {
        return MyNavigationVisitor(tailOnly)
    }

    override fun createCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiReturnStatement> {
        return MyCollectVisitor(at.project, mode, tailOnly)
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiReturnStatement>
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
    }

    private class MyCollectVisitor(
        private val project: Project,
        mode: Mode,
        private val tailOnly: Boolean
    ) : CollectVisitor<PsiReturnStatement>(mode) {
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
                Iterable { insns.iterator() }.lastOrNull(::insnHandler)
            } else {
                insns.iterator().forEach(::insnHandler)
            }
        }
    }
}

class ReturnInjectionPoint : AbstractReturnInjectionPoint(false)
class TailInjectionPoint : AbstractReturnInjectionPoint(true)
