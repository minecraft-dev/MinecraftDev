/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression

object FieldTargetReference : TargetReference.QualifiedHandler<PsiField>() {

    override fun createFindUsagesVisitor(
        context: PsiElement,
        targetClass: PsiClass,
        checkOnly: Boolean
    ): CollectVisitor<PsiReferenceExpression>? {
        return MixinMemberReference.parse(context.constantStringValue)
            ?.let({ FindUsagesVisitor(targetClass, it, checkOnly) })
    }

    override fun createCollectUsagesVisitor(): CollectVisitor<QualifiedMember<PsiField>> = CollectUsagesVisitor()

    override fun createLookup(targetClass: PsiClass, m: PsiField, owner: PsiClass): LookupElementBuilder {
        return JavaLookupElementBuilder.forField(
            m,
            MixinMemberReference.toString(m.getQualifiedMemberReference(owner)),
            targetClass
        )
            .withPresentableText(m.name)
            .withLookupString(m.name)
    }

    private class FindUsagesVisitor(
        private val targetClass: PsiClass,
        private val target: MemberReference,
        checkOnly: Boolean
    ) :
        CollectVisitor<PsiReferenceExpression>(checkOnly) {

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            if (expression !is PsiMethodReferenceExpression) {
                // TODO: Optimize this so we don't need to resolve all fields to find a reference
                val resolved = expression.resolve()
                if (resolved is PsiField && target.match(
                        resolved,
                        QualifiedMember.resolveQualifier(expression) ?: targetClass
                    )
                ) {
                    addResult(expression)
                }
            }

            super.visitReferenceExpression(expression)
        }
    }

    private class CollectUsagesVisitor : CollectVisitor<QualifiedMember<PsiField>>(false) {

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            if (expression !is PsiMethodReferenceExpression) {
                val resolved = expression.resolve()
                if (resolved is PsiField) {
                    addResult(QualifiedMember(resolved, expression))
                }
            }

            super.visitReferenceExpression(expression)
        }
    }
}
