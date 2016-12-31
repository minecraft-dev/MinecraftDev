/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.editor

import com.demonwav.mcdev.platform.mixin.util.MemberReference
import com.intellij.codeInsight.editorActions.ReferenceData
import com.intellij.psi.PsiElement

internal class MixinReferenceData(startOffset: Int, endOffset: Int, internal val reference: MemberReference)
    : ReferenceData(startOffset, endOffset, reference.owner!!, reference.name) {

    internal companion object {

        internal fun create(element: PsiElement, startOffset: Int, reference: MemberReference): MixinReferenceData {
            val range = element.textRange
            return MixinReferenceData(
                    range.startOffset - startOffset,
                    range.endOffset - startOffset,
                    reference)
        }

    }

}
