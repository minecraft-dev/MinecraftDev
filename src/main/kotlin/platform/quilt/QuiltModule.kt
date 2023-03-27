/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.fabric.reference.EntryPointReference
import com.demonwav.mcdev.platform.quilt.util.QuiltConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class QuiltModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var quiltJson by nullable { facet.findFile(QuiltConstants.QUILT_MOD_JSON, SourceType.RESOURCE) }
        private set

    override val moduleType = QuiltModuleType
    override val type = PlatformType.QUILT
    override val icon = PlatformAssets.QUILT_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val parent = identifier.uastParent
        if (parent !is UClass && parent !is UMethod) {
            return false
        }

        val psiParent = parent.sourcePsi
            ?: return false
        return ReferencesSearch.search(psiParent).anyMatch { EntryPointReference.isEntryPointReference(it) }
    }

    override fun dispose() {
        super.dispose()
        quiltJson = null
    }
}
