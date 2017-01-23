/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.actions.insertShadows
import com.demonwav.mcdev.util.getClassOfElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.lookup.VariableLookupItem
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import java.util.stream.Stream

internal class MixinMethodLookupItem(method: PsiMethod) : JavaMethodCallElement(method) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, `object`)
        super.handleInsert(context)
    }

}

internal class MixinFieldLookupItem(field: PsiField, private val qualified: Boolean) : VariableLookupItem(field) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, `object` as PsiMember)

        if (this.qualified) {
            super.handleInsert(context)
        } else {
            // Cannot call super because that would add a qualifier to the target class
            context.document.replaceString(context.startOffset, context.tailOffset, `object`.name!!)
            context.commitDocument()
        }
    }

}

private fun insertShadow(context: InsertionContext, member: PsiMember) {
    // Insert @Shadow element
    val psiClass = getClassOfElement(context.file.findElementAt(context.startOffset)) ?: return
    insertShadows(context.project, psiClass, Stream.of(member))
    PostprocessReformattingAspect.getInstance(context.project).doPostponedFormatting()
}
