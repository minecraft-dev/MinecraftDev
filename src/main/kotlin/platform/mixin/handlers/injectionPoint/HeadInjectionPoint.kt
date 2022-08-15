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
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement> {
        return MyCollectVisitor(at.project, targetClass, mode)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor {
        return MyNavigationVisitor()
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
        mode: Mode
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
