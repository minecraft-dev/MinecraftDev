/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.generation.SpongeEventGenerationPanel
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.intellij.psi.PsiClass

object SpongeModuleType : AbstractModuleType<SpongeModule>("org.spongepowered", "spongeapi") {

    private const val ID = "SPONGE_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = listOf(SpongeConstants.LISTENER_ANNOTATION, SpongeConstants.PLUGIN_ANNOTATION)
    val LISTENER_ANNOTATIONS = listOf(SpongeConstants.LISTENER_ANNOTATION)

    override fun getPlatformType() = PlatformType.SPONGE
    override fun getIcon() = PlatformAssets.SPONGE_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)
    override fun generateModule(facet: MinecraftFacet) = SpongeModule(facet)
    override fun isEventGenAvailable() = true
    override fun getEventGenerationPanel(chosenClass: PsiClass) = SpongeEventGenerationPanel(chosenClass)
}
