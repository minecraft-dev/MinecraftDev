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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode

class JumpInjectionPoint : InjectionPoint<PsiElement>() {
    private object Const {
        val VALID_OPCODES = setOf(
            Opcodes.IFEQ,
            Opcodes.IFNE,
            Opcodes.IFLT,
            Opcodes.IFGE,
            Opcodes.IFGT,
            Opcodes.IFLE,
            Opcodes.IF_ICMPEQ,
            Opcodes.IF_ICMPNE,
            Opcodes.IF_ICMPLT,
            Opcodes.IF_ICMPGE,
            Opcodes.IF_ICMPGT,
            Opcodes.IF_ICMPLE,
            Opcodes.IF_ACMPEQ,
            Opcodes.IF_ACMPNE,
            Opcodes.GOTO,
            Opcodes.JSR,
            Opcodes.IFNULL,
            Opcodes.IFNONNULL,
        )
    }

    override val discouragedMessage = "Usage of JUMP is discouraged because it is brittle"

    override val validOpcodes = Const.VALID_OPCODES

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        // TODO: jump target source navigation? This would be extremely hard
        return null
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement> {
        val opcode = (at.findDeclaredAttributeValue("opcode")?.constantValue as? Int)
            ?.takeIf { it in Const.VALID_OPCODES } ?: -1
        return MyCollectVisitor(at.project, targetClass, mode, opcode)
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>
    ): LookupElementBuilder? {
        return null
    }

    private class MyCollectVisitor(
        private val project: Project,
        private val clazz: ClassNode,
        mode: Mode,
        private val opcode: Int
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) = sequence {
            val insns = methodNode.instructions ?: return@sequence
            for (insn in insns) {
                if (insn is JumpInsnNode && (opcode == -1 || insn.opcode == opcode)) {
                    addResult(insn, methodNode.findOrConstructSourceMethod(clazz, project))
                }
            }
        }
    }
}
