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

package com.demonwav.mcdev.platform.velocity

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.generation.VelocityGenerationData
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants.SUBSCRIBE_ANNOTATION
import com.demonwav.mcdev.util.createVoidMethodWithParameterType
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class VelocityModule(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = VelocityModuleType
    override val type = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean = true

    override fun generateEventListenerMethod(
        containingClass: PsiClass,
        chosenClass: PsiClass,
        chosenName: String,
        data: GenerationData?,
    ): PsiMethod? {
        val method = createVoidMethodWithParameterType(project, chosenName, chosenClass) ?: return null
        val modifierList = method.modifierList

        val subscribeAnnotation = modifierList.addAnnotation(SUBSCRIBE_ANNOTATION)

        val generationData = data as VelocityGenerationData

        if (generationData.eventOrder != "NORMAL") {
            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(
                    "com.velocitypowered.api.event.PostOrder." + generationData.eventOrder,
                    subscribeAnnotation,
                )

            subscribeAnnotation.setDeclaredAttributeValue("order", value)
        }

        return method
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { identifier.uastParent as? UClass }
            ?: return false

        return !psiClass.hasModifier(JvmModifier.ABSTRACT) &&
            psiClass.findAnnotation(VelocityConstants.PLUGIN_ANNOTATION) != null
    }
}
