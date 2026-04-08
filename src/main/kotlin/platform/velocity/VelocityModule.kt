/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class VelocityModule(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = VelocityModuleType
    override val type = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON

    override fun computeModIds(): List<String> {
        val pluginAnnotation =
            JavaPsiFacade.getInstance(project).findClass(VelocityConstants.PLUGIN_ANNOTATION, GlobalSearchScope.allScope(project))
                ?: return emptyList()
        return AnnotatedElementsSearch.searchElements(pluginAnnotation, GlobalSearchScope.moduleScope(module), PsiClass::class.java)
            .mapNotNull { pluginClass ->
                pluginClass.findAnnotation(VelocityConstants.PLUGIN_ANNOTATION)?.findAttributeValue("id")?.constantStringValue
            }
    }

    override val eventListenerGenSupport: EventListenerGenerationSupport = VelocityEventListenerGenerationSupport()

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean = true

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { identifier.uastParent as? UClass }
            ?: return false

        return !psiClass.javaPsi.hasModifier(JvmModifier.ABSTRACT) &&
            psiClass.findAnnotation(VelocityConstants.PLUGIN_ANNOTATION) != null
    }
}
