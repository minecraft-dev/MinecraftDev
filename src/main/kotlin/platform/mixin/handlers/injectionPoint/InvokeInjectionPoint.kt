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
import com.demonwav.mcdev.util.MemberReference
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

abstract class AbstractInvokeInjectionPoint(private val assign: Boolean) : AbstractMethodInjectionPoint() {
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
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, at.project, MemberReference(""), assign)
        }
        return target?.let { MyCollectVisitor(mode, at.project, it, assign) }
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

        override fun visitNewExpression(expression: PsiNewExpression) {
            val constructor = expression.resolveConstructor()
            if (constructor != null) {
                visitMethodUsage(constructor, constructor.containingClass!!, expression)
            }

            super.visitNewExpression(expression)
        }

        override fun visitForeachStatement(statement: PsiForeachStatement) {
            // Enhanced for loops get compiled to a loop calling next on an Iterator
            // Since these method calls are not available in the method source we need
            // to generate 3 virtual method calls: the call to get the Iterator
            // (Iterable.iterator()) and "Iterator.next()" and "Iterator.hasNext()"

            val type = (statement.iteratedValue?.type as? PsiClassType)?.resolve()
            if (type != null) {
                // Find iterator() method
                val method = type.findMethodsByName("iterator", true).first { it.parameterList.parametersCount == 0 }
                if (method != null) {
                    visitMethodUsage(method, type, statement)
                }
            }

            // Get Iterator class to resolve next and hasNext
            val iteratorClass = JavaPsiFacade.getInstance(statement.project)
                .findClass(CommonClassNames.JAVA_UTIL_ITERATOR, statement.resolveScope)

            if (iteratorClass != null) {
                val hasNext =
                    iteratorClass.findMethodsByName("hasNext", false).first { it.parameterList.parametersCount == 0 }
                if (hasNext != null) {
                    visitMethodUsage(hasNext, iteratorClass, statement)
                }

                val next =
                    iteratorClass.findMethodsByName("next", false).first { it.parameterList.parametersCount == 0 }
                if (next != null) {
                    visitMethodUsage(next, iteratorClass, statement)
                }
            }

            super.visitForeachStatement(statement)
        }
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val project: Project,
        private val selector: MixinSelector,
        private val assign: Boolean,
    ) : CollectVisitor<PsiMethod>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is MethodInsnNode) {
                    return@forEachRemaining
                }

                val sourceMethod = nodeMatchesSelector(insn, mode, selector, project) ?: return@forEachRemaining
                val actualInsn = if (assign) insn.next else insn
                if (actualInsn != null) {
                    addResult(
                        actualInsn,
                        sourceMethod,
                        qualifier = insn.owner.replace('/', '.'),
                    )
                }
            }
        }
    }
}

class InvokeInjectionPoint : AbstractInvokeInjectionPoint(false)

class InvokeAssignInjectionPoint : AbstractInvokeInjectionPoint(true)
