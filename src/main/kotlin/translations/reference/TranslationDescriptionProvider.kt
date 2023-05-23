/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
