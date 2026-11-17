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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickParserDefinition.TokenSets
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class UnpickFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner =
        DefaultWordsScanner(UnpickLexerAdapter(), TokenSets.IDENTIFIERS, TokenSets.COMMENTS, TokenSets.STRINGS)

    override fun canFindUsagesFor(element: PsiElement) = element is UnpickGroupDefinition && element.name != null

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return if (element is UnpickGroupDefinition) MCDevBundle("minecraft.unpick.find_usages.group") else ""
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return (element as? UnpickGroupDefinition)?.name ?: ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)
}
