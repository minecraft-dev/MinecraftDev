/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.util.getQualifiedInternalNameAndDescriptor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNewExpression

internal class ConstructorTargetReference(element: PsiLiteral, methodReference: MixinReference)
    : BaseMethodTargetReference(element, methodReference) {

    override val description: String
        get() = "constructor target '$value' in target method"

    override fun createFindUsagesVisitor(): FindUsagesVisitor = FindConstructorUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectMethodsVisitor = CollectCalledConstructorsVisitor()

}

private class FindConstructorUsagesVisitor(val qinad: String) : FindUsagesVisitor() {

    override fun visitNewExpression(expression: PsiNewExpression) {
        val constructor = expression.resolveConstructor()
        if (constructor != null && constructor.getQualifiedInternalNameAndDescriptor(null) == this.qinad) {
            usages.add(expression)
        }

        super.visitNewExpression(expression)
    }

}

private class CollectCalledConstructorsVisitor : CollectMethodsVisitor() {

    override fun visitNewExpression(expression: PsiNewExpression) {
        val constructor = expression.resolveConstructor()
        if (constructor != null) {
            methods.add(QualifiedMember(constructor, null as PsiClassType?))
        }

        super.visitNewExpression(expression)
    }

}
