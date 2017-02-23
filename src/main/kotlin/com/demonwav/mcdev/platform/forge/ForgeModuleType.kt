/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass

object ForgeModuleType : AbstractModuleType<ForgeModule>("", "") {

    private const val ID = "FORGE_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = listOf(
        ForgeConstants.MOD_ANNOTATION,
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    )
    val LISTENER_ANNOTATIONS = listOf(
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    )

    override fun getPlatformType() = PlatformType.FORGE
    override fun getIcon() = PlatformAssets.FORGE_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS
    override fun generateModule(module: Module) = ForgeModule(module)
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)
    override fun isEventGenAvailable() = true
}
