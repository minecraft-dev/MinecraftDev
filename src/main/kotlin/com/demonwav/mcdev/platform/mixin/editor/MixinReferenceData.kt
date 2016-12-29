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

import com.demonwav.mcdev.util.MemberDescriptor
import com.intellij.codeInsight.editorActions.ReferenceData
import com.intellij.psi.PsiElement



internal class MixinReferenceData(startOffset: Int, endOffset: Int, val descriptor: MemberDescriptor)
    : ReferenceData(startOffset, endOffset, descriptor.owner!!, descriptor.name) {

    internal companion object {

        internal fun create(element: PsiElement, startOffset: Int, memberDescriptor: MemberDescriptor): MixinReferenceData {
            val range = element.textRange
            return MixinReferenceData(
                    range.startOffset - startOffset,
                    range.endOffset - startOffset,
                    memberDescriptor)
        }

    }

}
