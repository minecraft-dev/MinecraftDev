/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ReferencesSearch

class FabricModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var fabricJson by nullable { facet.findFile(FabricConstants.FABRIC_MOD_JSON, SourceType.RESOURCE) }
        private set

    override val moduleType = FabricModuleType
    override val type = PlatformType.FABRIC
    override val icon = PlatformAssets.FABRIC_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }
        val parent = element.parent
        if (parent !is PsiClass && parent !is PsiMethod) {
            return false
        }

        return ReferencesSearch.search(parent).anyMatch { EntryPointReference.isEntryPointReference(it) }
    }

    override fun dispose() {
        super.dispose()
        fabricJson = null
    }
}
