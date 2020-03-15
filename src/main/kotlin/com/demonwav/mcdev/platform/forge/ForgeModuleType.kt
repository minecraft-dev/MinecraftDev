/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.psi.PsiClass

object ForgeModuleType : AbstractModuleType<ForgeModule>("", "") {

    private const val ID = "FORGE_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(
        ForgeConstants.MOD_ANNOTATION,
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    )
    private val LISTENER_ANNOTATIONS = listOf(
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    )

    override val platformType = PlatformType.FORGE
    override val icon = PlatformAssets.FORGE_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet) = ForgeModule(facet)
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)

    val FG2_3_VERSION = SemanticVersion.release(1, 12)
    val FG3_VERSION = SemanticVersion.release(1, 13)
}
