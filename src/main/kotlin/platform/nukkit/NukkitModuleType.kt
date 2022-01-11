/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.nukkit.util.NukkitConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass
import platform.nukkit.generation.NukkitEventGenerationPanel

object NukkitModuleType : AbstractModuleType<NukkitModule<NukkitModuleType>>("cn.nukkit", "nukkit") {

    private const val ID = "NUKKIT_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(NukkitConstants.HANDLER_ANNOTATION)
    private val LISTENER_ANNOTATIONS = listOf(NukkitConstants.HANDLER_ANNOTATION)

    init {
        CommonColors.applyStandardColors(colorMap, NukkitConstants.TEXT_FORMAT_CLASS)
    }

    override val platformType = PlatformType.NUKKIT
    override val icon = PlatformAssets.NUKKIT_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = NukkitModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = NukkitEventGenerationPanel(chosenClass)
}
