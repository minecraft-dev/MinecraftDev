/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewTypeLocation

class TranslationDescriptionProvider : ElementDescriptionProvider {
    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val file = element.containingFile?.virtualFile
        if (!TranslationFiles.isTranslationFile(file)) {
            return null
        }
        return when (element) {
            is LangEntry -> if (location is UsageViewTypeLocation) "translation" else element.key
            is JsonProperty -> if (location is UsageViewTypeLocation) "translation" else element.name
            else -> null
        }
    }
}
