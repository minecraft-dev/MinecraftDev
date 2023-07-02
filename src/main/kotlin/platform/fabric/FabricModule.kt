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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.fabric.reference.EntryPointReference
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class FabricModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var fabricJson by nullable { facet.findFile(FabricConstants.FABRIC_MOD_JSON, SourceType.RESOURCE) }
        private set

    override val moduleType = FabricModuleType
    override val type = PlatformType.FABRIC
    override val icon = PlatformAssets.FABRIC_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val parent = runCatchingKtIdeaExceptions { identifier.uastParent }
        if (parent !is UClass && parent !is UMethod) {
            return false
        }

        val psiParent = parent.sourcePsi
            ?: return false
        return ReferencesSearch.search(psiParent).anyMatch { EntryPointReference.isEntryPointReference(it) }
    }

    override fun dispose() {
        super.dispose()
        fabricJson = null
    }
}
