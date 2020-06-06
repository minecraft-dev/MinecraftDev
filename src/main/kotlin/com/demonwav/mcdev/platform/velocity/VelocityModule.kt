/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class VelocityModule(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = VelocityModuleType
    override val type = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean = true

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        val psiClass = element.parent as? PsiClass ?: return false
        return psiClass.hasAnnotation(VelocityConstants.PLUGIN_ANNOTATION)
    }
}
