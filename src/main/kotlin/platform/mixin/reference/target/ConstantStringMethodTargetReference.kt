/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

object ConstantStringMethodTargetReference : TargetReference.MethodHandler() {
    override fun createNavigationVisitor(
        context: PsiElement,
        targetClass: PsiClass
    ): NavigationVisitor? {
        return MixinMemberReference.parse(context.constantStringValue)
            ?.let { MyNavigationVisitor(targetClass, it) }
    }

    override fun createCollectVisitor(
        context: PsiElement,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiMethod>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, context.project, MemberReference(""))
        }
        return MixinMemberReference.parse(context.constantStringValue)
            ?.let { MyCollectVisitor(mode, context.project, it) }
    }

    private fun isConstantStringMethodCall(expression: PsiMethodCallExpression): Boolean {
        // Must return void
        if (expression.type != PsiType.VOID) {
            return false
        }

        val arguments = expression.argumentList
        val argumentTypes = arguments.expressionTypes
        val javaStringType = PsiType.getJavaLangString(
            expression.manager,
            expression.resolveScope
        )

        if (argumentTypes.size != 1 || argumentTypes[0] != javaStringType) {
            // Must have one String parameter
            return false
        }

        // Expression must be constant, so either a literal or a constant field reference
        return when (val expr = arguments.expressions[0]) {
            is PsiLiteral -> true
            is PsiReference -> (expr.resolve() as? PsiVariable)?.computeConstantValue() != null
            else -> false
        }
    }

    private class MyNavigationVisitor(
        private val targetClass: PsiClass,
        private val target: MemberReference
    ) : NavigationVisitor() {

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (isConstantStringMethodCall(expression)) {
                expression.resolveMethod()?.let { method ->
                    val matches = target.match(
                        method,
                        QualifiedMember.resolveQualifier(expression.methodExpression) ?: targetClass
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
        private val target: MemberReference
    ) : CollectVisitor<PsiMethod>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            var seenStringConstant = false
            insns.iterator().forEachRemaining { insn ->
                if (insn is MethodInsnNode) {
                    // make sure we're coming from a string constant
                    if (seenStringConstant) {
                        processMethodInsn(insn)
                    }
                }
                if ((insn as? LdcInsnNode)?.cst is String) {
                    seenStringConstant = true
                } else if (insn.opcode != -1) {
                    seenStringConstant = false
                }
            }
        }

        private fun processMethodInsn(insn: MethodInsnNode) {
            // must take a string and return void
            if (insn.desc != "(Ljava/lang/String;)V") return

            if (mode != Mode.COMPLETION) {
                // ensure we match the target
                if (!target.matchAllNames && target.name != insn.name) return
                if (target.descriptor != null && target.descriptor != insn.desc) return

                val owner = target.owner
                if (owner != null && owner.replace('.', '/') != insn.owner) return
            }

            val fakeMethod = insn.fakeResolve()
            addResult(
                fakeMethod.method.findOrConstructSourceMethod(
                    fakeMethod.clazz,
                    project,
                    canDecompile = false
                ),
                qualifier = insn.owner.replace('/', '.')
            )
        }
    }
}
