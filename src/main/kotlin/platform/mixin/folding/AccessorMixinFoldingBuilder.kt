/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.handlers.AccessorHandler
import com.demonwav.mcdev.platform.mixin.handlers.InvokerHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.util.referencedMethod
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType

class AccessorMixinFoldingBuilder : CustomFoldingBuilder() {

    override fun isDumbAware(): Boolean = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        val settings = MixinFoldingSettings.instance.state
        val element = node.psi
        val parent = element.parentOfType<PsiMethodCallExpression>() ?: return false
        val refMethod = parent.referencedMethod

        return when {
            refMethod == null -> false
            refMethod.hasAnnotation(INVOKER) -> when (element) {
                is PsiIdentifier -> settings.foldInvokerMethodCalls
                is PsiParenthesizedExpression -> settings.foldInvokerCasts
                else -> false
            }
            refMethod.hasAnnotation(ACCESSOR) -> when (element) {
                is PsiIdentifier -> settings.foldAccessorMethodCalls
                is PsiParenthesizedExpression -> settings.foldAccessorCasts
                else -> false
            }
            else -> false
        }
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        // Accessor parentheses
        if (node.elementType == JavaTokenType.LPARENTH || node.elementType == JavaTokenType.RPARENTH) {
            return ""
        }

        return when (val element = node.psi) {
            is PsiParenthesizedExpression -> foldAccessorCastExpression(element)
            is PsiIdentifier -> foldAccessorIdentifier(element)
            else -> null
        } ?: node.text
    }

    private fun foldAccessorIdentifier(identifier: PsiIdentifier): String? {
        val expr = identifier.parentOfType<PsiMethodCallExpression>() ?: return null
        val method = expr.referencedMethod ?: return null

        if (method.hasAnnotation(INVOKER)) {
            return InvokerHandler.getInstance()?.findInvokerTargetForReference(method)?.element?.name
        }
        if (method.hasAnnotation(ACCESSOR)) {
            val name = AccessorHandler.getInstance()?.findAccessorTargetForReference(method)?.element?.name
                ?: return null
            return if (method.returnType == PsiType.VOID) {
                "$name = "
            } else {
                name
            }
        }

        return null
    }

    private fun foldAccessorCastExpression(expr: PsiParenthesizedExpression): String? {
        val castExpression = expr.expression as? PsiTypeCastExpression ?: return null
        return castExpression.operand?.text
    }

    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        if (root !is PsiJavaFile || !MixinModuleType.isInModule(root)) {
            return
        }

        root.accept(Visitor(descriptors))
    }

    private class Visitor(private val descriptors: MutableList<FoldingDescriptor>) :
        JavaRecursiveElementWalkingVisitor() {

        val settings = MixinFoldingSettings.instance.state

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            super.visitMethodCallExpression(expression)

            if (
                !settings.foldInvokerCasts && !settings.foldInvokerMethodCalls &&
                !settings.foldAccessorCasts && !settings.foldAccessorMethodCalls
            ) {
                return
            }

            // This is what we're actually going to fold, not the PsiMethodCallExpression
            val referenceExpression = expression.methodExpression

            val refMethod = expression.referencedMethod ?: return

            val invoker = refMethod.hasAnnotation(INVOKER)
            if (invoker && !settings.foldInvokerCasts && !settings.foldInvokerMethodCalls) {
                return
            }
            val accessor = refMethod.hasAnnotation(ACCESSOR)
            if (accessor && !settings.foldAccessorCasts && !settings.foldAccessorMethodCalls) {
                return
            }

            // The final element must be a method name (identifier)
            val identifier = referenceExpression.lastChild as? PsiIdentifier ?: return
            if (invoker && settings.foldInvokerMethodCalls) {
                descriptors.add(
                    FoldingDescriptor(
                        identifier.node,
                        identifier.textRange
                    )
                )
            }

            if (accessor && settings.foldAccessorMethodCalls) {
                foldAccessorMethodCall(expression, identifier)
            }

            // We have folded the method call by this point
            // Now we need to decide to fold the cast expression
            // Note: There may not be a cast expression

            // The pattern we're expecting is something like:
            //     ((MixinWorldAccessor) world).callSomeMethod(...)
            // So if there's a cast expression here it'll be inside a parenthisized expression
            val parenthetical = referenceExpression.firstChild as? PsiParenthesizedExpression ?: return
            val castExpression = parenthetical.expression as? PsiTypeCastExpression ?: return

            // Can't fold without an operand
            if (castExpression.operand == null) {
                return
            }

            if ((invoker && settings.foldInvokerCasts) || (accessor && settings.foldAccessorCasts)) {
                descriptors.add(
                    FoldingDescriptor(
                        parenthetical.node,
                        parenthetical.textRange
                    )
                )
            }
        }

        private fun foldAccessorMethodCall(
            expression: PsiMethodCallExpression,
            identifier: PsiIdentifier
        ) {
            val argumentList = expression.argumentList
            val openParen = argumentList.firstChild
            val closeParen = argumentList.lastChild
            if (openParen.elementType !== JavaTokenType.LPARENTH || closeParen.elementType !== JavaTokenType.RPARENTH) {
                return
            }

            // All of these folds go together
            val group = FoldingGroup.newGroup("accessMethodCall")

            // For a getter we could do both () in a single fold
            // but for a setter we have to do them separate (since there's an expression in side of them)
            // In practice it doesn't really matter, so just always handling them separate reduce's the number of
            // special cases the code has to understand.
            descriptors.add(
                FoldingDescriptor(
                    openParen.node,
                    openParen.textRange,
                    group
                )
            )
            descriptors.add(
                FoldingDescriptor(
                    closeParen.node,
                    closeParen.textRange,
                    group
                )
            )

            descriptors.add(
                FoldingDescriptor(
                    identifier.node,
                    identifier.textRange,
                    group
                )
            )
        }
    }
}
