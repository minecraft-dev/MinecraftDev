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
import com.demonwav.mcdev.platform.mixin.util.MemberInfo
import com.demonwav.mcdev.util.Quantifier
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.util.ArrayUtilRt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.util.Printer

class InvokeAssignInjectionPoint : AbstractMethodInjectionPoint() {
    private object Const {
        val ARGS_KEYS = arrayOf("fuzz", "skip")
        val SKIP_LIST_DELIMITER = "[ ,;]".toRegex()
        val OPCODES_BY_NAME = Printer.OPCODES.withIndex().associate { it.value to it.index }
        val DEFAULT_SKIP = setOf(
            // Opcodes which may appear if the targetted method is part of an
            // expression eg. int foo = 2 + this.bar();
            Opcodes.DUP, Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
            Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB, Opcodes.IMUL,
            Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL, Opcodes.IDIV, Opcodes.LDIV,
            Opcodes.FDIV, Opcodes.DDIV, Opcodes.IREM, Opcodes.LREM, Opcodes.FREM,
            Opcodes.DREM, Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG,
            Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR,
            Opcodes.LUSHR, Opcodes.IAND, Opcodes.LAND, Opcodes.IOR, Opcodes.LOR,
            Opcodes.IXOR, Opcodes.LXOR, Opcodes.IINC,

            // Opcodes which may appear if the targetted method is cast before
            // assignment eg. int foo = (int)this.getFloat();
            Opcodes.I2L, Opcodes.I2F, Opcodes.I2D, Opcodes.L2I, Opcodes.L2F,
            Opcodes.L2D, Opcodes.F2I, Opcodes.F2L, Opcodes.F2D, Opcodes.D2I,
            Opcodes.D2L, Opcodes.D2F, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
            Opcodes.CHECKCAST, Opcodes.INSTANCEOF
        )
    }

    override fun onCompleted(editor: Editor, reference: PsiLiteral) {
        completeExtraStringAtAttribute(editor, reference, "target")
    }

    override fun getArgsKeys(at: PsiAnnotation) = Const.ARGS_KEYS

    override fun getArgsValues(at: PsiAnnotation, key: String): Array<out Any> {
        if (key == "skip") {
            return Printer.OPCODES
        }
        return ArrayUtilRt.EMPTY_OBJECT_ARRAY
    }

    override fun getArgValueListDelimiter(at: PsiAnnotation, key: String) =
        Const.SKIP_LIST_DELIMITER.takeIf { key == "skip" }

    override fun isShiftDiscouraged(shift: Int, at: PsiAnnotation): Boolean {
        // Allow shifting before INVOKE_ASSIGN
        return shift != 0 && shift != -1
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor? {
        return target?.let { MyNavigationVisitor(targetClass, it) }
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<PsiMethod>? {
        val args = AtResolver.getArgs(at)
        val fuzz = args["fuzz"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val skip = args["skip"]?.let { parseSkip(it) } ?: Const.DEFAULT_SKIP

        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, at.project, MemberInfo(), fuzz, skip)
        }
        return target?.let { MyCollectVisitor(mode, at.project, it, fuzz, skip) }
    }

    private fun parseSkip(string: String): Set<Int> {
        return string.split(Const.SKIP_LIST_DELIMITER)
            .asSequence()
            .mapNotNull { part ->
                val trimmedPart = part.trim()
                Const.OPCODES_BY_NAME[trimmedPart.removePrefix("Opcodes.")]
                    ?: trimmedPart.toIntOrNull()?.takeIf { it >= 0 && it < Printer.OPCODES.size }
            }
            .toSet()
    }

    private class MyNavigationVisitor(
        private val targetClass: PsiClass,
        private val selector: MixinSelector,
    ) : NavigationVisitor() {

        private fun visitMethodUsage(method: PsiMethod, qualifier: PsiClass?, expression: PsiElement) {
            if (selector.matchMethod(method, qualifier ?: targetClass)) {
                addResult(expression)
            }
        }

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            val method = expression.resolveMethod()
            if (method != null) {
                val containingClass = method.containingClass

                // Normally, Java uses the type of the instance to qualify the method calls
                // However, if the method is part of java.lang.Object (e.g. equals or toString)
                // and no class in the hierarchy of the instance overrides the method, Java will
                // insert the call using java.lang.Object as the owner
                val qualifier =
                    if (method.isConstructor || containingClass?.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                        containingClass
                    } else {
                        QualifiedMember.resolveQualifier(expression.methodExpression)
                    }

                visitMethodUsage(method, qualifier, expression)
            }

            super.visitMethodCallExpression(expression)
        }
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val project: Project,
        private val selector: MixinSelector,
        private val fuzz: Int,
        private val skip: Set<Int>,
    ) : CollectVisitor<PsiMethod>(mode) {
        override val quantifier: Quantifier
            get() = selector.quantifier

        override fun accept(methodNode: MethodNode) = sequence {
            val insns = methodNode.instructions ?: return@sequence
            for (insn in insns) {
                if (insn !is MethodInsnNode) {
                    continue
                }

                val sourceMethod = nodeMatchesSelector(insn, mode, selector, project) ?: continue

                val offset = insns.indexOf(insn)
                val maxOffset = (offset + fuzz + 1).coerceAtMost(insns.size())
                var resultingInsn = insn
                for (i in offset + 1 until maxOffset) {
                    val candidate = insns[i]
                    if (candidate is VarInsnNode && candidate.opcode >= Opcodes.ISTORE) {
                        resultingInsn = candidate
                        break
                    } else if (skip.isNotEmpty() && candidate.opcode !in skip) {
                        break
                    }
                }

                resultingInsn = resultingInsn.next ?: resultingInsn

                addResult(
                    resultingInsn,
                    sourceMethod,
                    qualifier = insn.owner.replace('/', '.'),
                )
            }
        }
    }
}
