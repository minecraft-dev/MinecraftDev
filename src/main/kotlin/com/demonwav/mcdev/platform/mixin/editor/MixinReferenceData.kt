/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.editor

import com.intellij.codeInsight.editorActions.ReferenceData
import com.intellij.psi.PsiElement



internal class MixinReferenceData(startOffset: Int, endOffset: Int, qClassName: String?, staticMemberDescriptor: String?)
    : ReferenceData(startOffset, endOffset, qClassName, staticMemberDescriptor) {

    internal companion object {

        internal fun create(element: PsiElement, startOffset: Int,
                                         qClassName: String, staticMemberDescriptor: String?): ReferenceData {
            val range = element.textRange
            return MixinReferenceData(
                    range.startOffset - startOffset,
                    range.endOffset - startOffset,
                    qClassName, staticMemberDescriptor)
        }

    }

}
