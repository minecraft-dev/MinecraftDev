/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.action.disableAnnotationWrapping
import com.demonwav.mcdev.platform.mixin.action.insertShadows
import com.demonwav.mcdev.platform.mixin.util.ShadowTarget
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.VariableLookupItem
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import java.util.stream.Stream

fun ShadowTarget.createLookupElement(): LookupElement {
    return when (member) {
        is PsiMethod -> MixinMethodLookupItem(this)
        is PsiField -> MixinFieldLookupItem(this)
        else -> throw AssertionError()
    }
}

private class MixinMethodLookupItem(private val shadow: ShadowTarget) :
    JavaMethodCallElement(shadow.member as PsiMethod) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, shadow)
        super.handleInsert(context)
    }
}

private class MixinFieldLookupItem(private val shadow: ShadowTarget) : VariableLookupItem(shadow.member as PsiField) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, shadow)

        // Replace object with proxy object so super doesn't qualify the reference
        `object` = ShadowField(`object`)
        super.handleInsert(context)
    }

    private class ShadowField(variable: PsiVariable) : PsiVariable by variable
}

private fun insertShadow(context: InsertionContext, shadow: ShadowTarget) {
    val mixinClass = shadow.mixin ?: context.file.findElementAt(context.startOffset)?.findContainingClass() ?: return

    // Insert @Shadow element
    insertShadows(context.project, mixinClass, Stream.of(shadow.member))
    disableAnnotationWrapping(context.project) {
        PostprocessReformattingAspect.getInstance(context.project).doPostponedFormatting()
    }

    if (shadow.mixin == null) {
        context.commitDocument()
    } else {
        context.setLaterRunnable {
            HintManager.getInstance().showInformationHint(
                context.editor,
                "Added @Shadow for '${shadow.member.name}' to super mixin ${mixinClass.shortName}"
            )
        }
    }
}
