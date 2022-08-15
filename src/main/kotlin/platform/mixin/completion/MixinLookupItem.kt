/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.action.disableAnnotationWrapping
import com.demonwav.mcdev.platform.mixin.action.insertShadows
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.VariableLookupItem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PostprocessReformattingAspect

fun MixinTargetMember.createLookupElement(project: Project): LookupElement {
    return when (this) {
        is MethodTargetMember -> MixinMethodLookupItem.create(project, this)
        is FieldTargetMember -> MixinFieldLookupItem.create(project, this)
    }
}

private class MixinMethodLookupItem(private val shadow: MethodTargetMember, private val method: PsiMethod) :
    JavaMethodCallElement(method) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, shadow, method)
        super.handleInsert(context)
    }

    companion object {
        fun create(project: Project, shadow: MethodTargetMember): MixinMethodLookupItem {
            val psiMethod = shadow.classAndMethod.method.findOrConstructSourceMethod(
                shadow.classAndMethod.clazz,
                project,
                canDecompile = false
            )
            return MixinMethodLookupItem(shadow, psiMethod)
        }
    }
}

private class MixinFieldLookupItem(
    private val shadow: FieldTargetMember,
    private val field: PsiField
) : VariableLookupItem(field) {

    override fun handleInsert(context: InsertionContext) {
        insertShadow(context, shadow, field)

        // Replace object with proxy object so super doesn't qualify the reference
        `object` = ShadowField(`object`)
        super.handleInsert(context)
    }

    private class ShadowField(variable: PsiVariable) : PsiVariable by variable

    companion object {
        fun create(project: Project, shadow: FieldTargetMember): MixinFieldLookupItem {
            val psiField = shadow.classAndField.field.findOrConstructSourceField(
                shadow.classAndField.clazz,
                project,
                canDecompile = false
            )
            return MixinFieldLookupItem(shadow, psiField)
        }
    }
}

private fun insertShadow(context: InsertionContext, shadow: MixinTargetMember, element: PsiMember) {
    val mixinClass = shadow.mixin ?: context.file.findElementAt(context.startOffset)?.findContainingClass() ?: return

    // Insert @Shadow element
    insertShadows(context.project, mixinClass, sequenceOf(element))
    disableAnnotationWrapping(context.project) {
        PostprocessReformattingAspect.getInstance(context.project).doPostponedFormatting()
    }

    if (shadow.mixin == null) {
        context.commitDocument()
    } else {
        context.setLaterRunnable {
            HintManager.getInstance().showInformationHint(
                context.editor,
                "Added @Shadow for '${element.name}' to super Mixin ${mixinClass.shortName}"
            )
        }
    }
}
