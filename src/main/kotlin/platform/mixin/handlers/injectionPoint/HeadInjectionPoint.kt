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
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiStatement
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class HeadInjectionPoint : InjectionPoint<PsiElement>() {
    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<PsiElement> {
        return MyCollectVisitor(at.project, targetClass, mode)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor {
        return MyNavigationVisitor()
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>,
    ): LookupElementBuilder? {
        return null
    }

    internal open class MyCollectVisitor(
        protected val project: Project,
        protected val clazz: ClassNode,
        mode: Mode,
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            val firstInsn = Iterable { insns.iterator() }.firstOrNull { it.opcode >= 0 } ?: return
            addResult(firstInsn, methodNode.findOrConstructSourceMethod(clazz, project))
        }
    }

    private class MyNavigationVisitor : NavigationVisitor() {
        private var firstStatement = true

        override fun visitStatement(statement: PsiStatement) {
            if (firstStatement) {
                firstStatement = false
                addResult(statement)
            }
            super.visitStatement(statement)
        }

        override fun visitExpression(expression: PsiExpression) {
            if (firstStatement) {
                // possible in lambda expressions
                firstStatement = false
                addResult(expression)
            }
            super.visitExpression(expression)
        }
    }
}
