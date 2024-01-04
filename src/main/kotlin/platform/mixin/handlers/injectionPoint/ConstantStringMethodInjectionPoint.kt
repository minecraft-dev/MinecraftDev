/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ConstantStringMethodInjectionPoint : AbstractMethodInjectionPoint() {
    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor? {
        return target?.let { MyNavigationVisitor(targetClass, it, AtResolver.getArgs(at)["ldc"]) }
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<PsiMethod>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, at.project, MemberReference(""), null)
        }
        return target?.let { MyCollectVisitor(mode, at.project, it, AtResolver.getArgs(at)["ldc"]) }
    }

    private class MyNavigationVisitor(
        private val targetClass: PsiClass,
        private val selector: MixinSelector,
        private val ldc: String?,
    ) : NavigationVisitor() {
        private fun isConstantStringMethodCall(expression: PsiMethodCallExpression): Boolean {
            // Must return void
            if (expression.type != PsiTypes.voidType()) {
                return false
            }

            val arguments = expression.argumentList
            val argumentTypes = arguments.expressionTypes
            val javaStringType = PsiType.getJavaLangString(
                expression.manager,
                expression.resolveScope,
            )

            if (argumentTypes.size != 1 || argumentTypes[0] != javaStringType) {
                // Must have one String parameter
                return false
            }

            // Expression must be constant
            val constantValue = arguments.expressions[0].constantStringValue ?: return false
            return ldc == null || ldc == constantValue
        }

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (isConstantStringMethodCall(expression)) {
                expression.resolveMethod()?.let { method ->
                    val matches = selector.matchMethod(
                        method,
                        QualifiedMember.resolveQualifier(expression.methodExpression) ?: targetClass,
                    )
                    if (matches) {
                        addResult(expression)
                    }
                }
            }

            super.visitMethodCallExpression(expression)
        }
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val project: Project,
        private val selector: MixinSelector,
        private val ldc: String?,
    ) : CollectVisitor<PsiMethod>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            var seenStringConstant: String? = null
            insns.iterator().forEachRemaining { insn ->
                if (insn is MethodInsnNode) {
                    // make sure we're coming from a string constant
                    if (seenStringConstant != null) {
                        if (ldc == null || ldc == seenStringConstant) {
                            processMethodInsn(insn)
                        }
                    }
                }
                val cst = (insn as? LdcInsnNode)?.cst
                if (cst is String) {
                    seenStringConstant = cst
                } else if (insn.opcode != -1) {
                    seenStringConstant = null
                }
            }
        }

        private fun processMethodInsn(insn: MethodInsnNode) {
            // must take a string and return void
            if (insn.desc != "(Ljava/lang/String;)V") return

            if (mode != Mode.COMPLETION) {
                // ensure we match the target
                if (!selector.matchMethod(insn.owner, insn.name, insn.desc)) {
                    return
                }
            }

            val fakeMethod = insn.fakeResolve()
            addResult(
                insn,
                fakeMethod.method.findOrConstructSourceMethod(
                    fakeMethod.clazz,
                    project,
                    canDecompile = false,
                ),
                qualifier = insn.owner.replace('/', '.'),
            )
        }
    }
}
