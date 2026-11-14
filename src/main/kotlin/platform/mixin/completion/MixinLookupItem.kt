/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.action.createShadowMembers
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
                canDecompile = false,
            )
            return MixinMethodLookupItem(shadow, psiMethod)
        }
    }
}

private class MixinFieldLookupItem(
    private val shadow: FieldTargetMember,
    private val field: PsiField,
) : VariableLookupItem(field) {

    override fun handleInsert(context: InsertionContext) {
        val shadowField = insertShadow(context, shadow, field) as? PsiField

        // Point super at the new @Shadow field rather than the target class field. Otherwise, super sees that the
        // inserted reference doesn't resolve to the target class field, and qualifies it with the target class name.
        if (shadowField != null) {
            `object` = shadowField
        }
        super.handleInsert(context)
    }

    companion object {
        fun create(project: Project, shadow: FieldTargetMember): MixinFieldLookupItem {
            val psiField = shadow.classAndField.field.findOrConstructSourceField(
                shadow.classAndField.clazz,
                project,
                canDecompile = false,
            )
            return MixinFieldLookupItem(shadow, psiField)
        }
    }
}

private fun insertShadow(context: InsertionContext, shadow: MixinTargetMember, element: PsiMember): PsiMember? {
    val mixinClass = shadow.mixin ?: context.file.findElementAt(context.startOffset)?.findContainingClass() ?: return null

    // Insert @Shadow element
    val shadowMembers = createShadowMembers(context.project, mixinClass, sequenceOf(element))
    insertShadows(mixinClass, shadowMembers)
    disableAnnotationWrapping(context.project) {
        PostprocessReformattingAspect.getInstance(context.project).doPostponedFormatting()
    }

    if (shadow.mixin == null) {
        context.commitDocument()
    } else {
        context.setLaterRunnable {
            HintManager.getInstance().showInformationHint(
                context.editor,
                "Added @Shadow for '${element.name}' to super Mixin ${mixinClass.shortName}",
            )
        }
    }

    return shadowMembers.singleOrNull()?.psiMember
}
