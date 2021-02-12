/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

class VelocityModule(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = VelocityModuleType
    override val type = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean = true

    override fun generateEventListenerMethod(
        containingClass: PsiClass,
        chosenClass: PsiClass,
        chosenName: String,
        data: GenerationData?
    ): PsiMethod? {
        val method = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID)
        val parameterList = method.parameterList

        val qName = chosenClass.qualifiedName ?: return null
        val parameter = JavaPsiFacade.getElementFactory(project)
            .createParameter(
                "event",
                PsiClassType.getTypeByName(qName, project, GlobalSearchScope.allScope(project))
            )

        parameterList.add(parameter)
        val modifierList = method.modifierList

        val subscribeAnnotation = modifierList.addAnnotation(SUBSCRIBE_ANNOTATION)

        val generationData = data as VelocityGenerationData

        if (generationData.eventOrder != "NORMAL") {
            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(
                    "com.velocitypowered.api.event.PostOrder." + generationData.eventOrder,
                    subscribeAnnotation
                )

            subscribeAnnotation.setDeclaredAttributeValue("order", value)
        }

        return method
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        val psiClass = element.parent as? PsiClass ?: return false
        return psiClass.hasAnnotation(VelocityConstants.PLUGIN_ANNOTATION)
    }
}
