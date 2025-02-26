/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.childLeafs
import com.intellij.psi.util.elementType
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

    override fun isInjectingAfter(at: PsiAnnotation) = true

    override fun createTargetInlay(
        at: PsiAnnotation,
        context: MixinAnnotationHandler.TargetInlayContext,
    ): MixinAnnotationHandler.TargetInlayProperties? {
        val inlayProps = super.createTargetInlay(at, context) ?: return null
        if (context.targetElement.elementType == JavaTokenType.LBRACE) {
            val parent = context.targetElement.parent
            if (parent is PsiCodeBlock) {
                val firstStatement = parent.statements.firstOrNull()
                if (firstStatement != null) {
                    return inlayProps.copy(
                        anchor = firstStatement,
                        placement = MixinAnnotationHandler.TargetInlayPlacement.PREVIOUS_LINE
                    )
                }
            }

            return inlayProps.copy(placement = MixinAnnotationHandler.TargetInlayPlacement.NEXT_LINE)
        }
        return inlayProps
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
        private var visitedAny = false

        override fun visitStart(executableElement: PsiElement) {
            val startElement = when (executableElement) {
                is PsiMethod -> executableElement.body?.lBrace
                is PsiLambdaExpression -> (executableElement.body as? PsiCodeBlock)?.lBrace
                    ?: executableElement.childLeafs().firstOrNull {
                        it is PsiJavaToken && it.elementType == JavaTokenType.ARROW
                    }
                is PsiClassInitializer -> executableElement.body.lBrace
                is PsiMethodReferenceExpression -> executableElement
                else -> null
            }

            if (startElement != null) {
                visitedAny = true
                addResult(startElement)
            }
        }

        override fun visitEnd(executableElement: PsiElement) {
            if (!visitedAny) {
                if (executableElement is PsiClass) {
                    val lBrace = executableElement.lBrace
                    if (lBrace != null) {
                        visitedAny = true
                        addResult(lBrace)
                    }
                }
                if (!visitedAny) {
                    addResult(executableElement)
                }
            }
        }

        override fun visitStatement(statement: PsiStatement) {
            if (!visitedAny) {
                visitedAny = true
                addResult(statement)
            }
            super.visitStatement(statement)
        }

        override fun visitExpression(expression: PsiExpression) {
            if (!visitedAny) {
                visitedAny = true
                addResult(expression)
            }
            super.visitExpression(expression)
        }
    }
}
