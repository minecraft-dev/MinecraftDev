/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.liteloader.util.LiteLoaderConstants
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class LiteLoaderModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    private var litemodJson: VirtualFile? = null

    init {
        setup()
    }

    private fun setup() {
        litemodJson = facet.findFile(LiteLoaderConstants.LITEMOD_JSON, SourceType.RESOURCE)
    }

    fun getLitemodJson(): VirtualFile? {
        if (litemodJson == null) {
            setup()
        }
        return litemodJson
    }

    override val moduleType = LiteLoaderModuleType
    override val type = PlatformType.LITELOADER
    override val icon = PlatformAssets.LITELOADER_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?) =
        element is PsiIdentifier &&
            element.parent is PsiClass &&
            element.text.startsWith("LiteMod")

    override fun dispose() {
        super.dispose()
        litemodJson = null
    }
}
