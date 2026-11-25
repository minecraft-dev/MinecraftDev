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

package com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.impl

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickGroups
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickGroupRefMixin
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiModificationTracker

abstract class UnpickGroupRefImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickGroupRefMixin {

    override fun resolveDefinitions(): List<UnpickGroupDefinition> = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        UnpickGroups.findDefinitions(this, text)
    }

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference = this

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        PsiElementResolveResult.createResults(resolveDefinitions())

    // Return the first definition rather than null when there are several, so that the group can be renamed from a
    // reference. Rename then picks up the other definitions through UnpickRenameGroupProcessor.
    override fun resolve(): PsiElement? = resolveDefinitions().firstOrNull()

    override fun getVariants(): Array<Any> {
        return UnpickGroups.findAllGroupNames(this).map {
            LookupElementBuilder.create(it).withIcon(PlatformAssets.MINECRAFT_ICON)
        }.toTypedArray()
    }

    override fun getRangeInElement(): TextRange = TextRange(0, textLength)

    override fun getCanonicalText(): String = text

    override fun handleElementRename(newElementName: String): PsiElement {
        firstChild.replace(UnpickElementFactory.createIdentifier(project, newElementName))
        return this
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        if (element is UnpickGroupDefinition) {
            element.name?.let { return handleElementRename(it) }
        }
        return this
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is UnpickGroupDefinition && element.name == text &&
            resolveDefinitions().any { it equivalentTo element }
    }

    override fun isSoft(): Boolean = false
}
