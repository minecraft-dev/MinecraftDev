/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.color

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.insight.ColorAnnotator
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class SpongeColorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!MinecraftSettings.instance.isShowChatColorUnderlines) {
            return
        }

        val pair = element.findColor() ?: return

        ColorAnnotator.setColorAnnotator(pair.first, element, holder)
    }
}
