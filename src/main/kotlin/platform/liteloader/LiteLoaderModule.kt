/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.liteloader.util.LiteLoaderConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class LiteLoaderModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var litemodJson by nullable { facet.findFile(LiteLoaderConstants.LITEMOD_JSON, SourceType.RESOURCE) }
        private set

    override val moduleType = LiteLoaderModuleType
    override val type = PlatformType.LITELOADER
    override val icon = PlatformAssets.LITELOADER_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false
        return identifier.uastParent is UClass && identifier.name.startsWith("LiteMod")
    }

    override fun dispose() {
        super.dispose()
        litemodJson = null
    }
}
