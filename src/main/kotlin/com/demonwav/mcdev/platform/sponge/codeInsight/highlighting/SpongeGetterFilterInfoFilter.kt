/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.codeInsight.highlighting

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.intellij.codeInsight.daemon.JavaErrorMessages
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile

class SpongeGetterFilterInfoFilter : HighlightInfoFilter {

    private val expectedDescription = JavaErrorMessages.message("annotation.missing.attribute", "'value'")!!

    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        if (file == null || highlightInfo.severity != HighlightSeverity.ERROR) {
            return true
        }

        val element = file.findReferenceAt(highlightInfo.actualStartOffset) ?: return true
        return element.canonicalText != SpongeConstants.GETTER_ANNOTATION ||
            highlightInfo.description != expectedDescription
    }
}
