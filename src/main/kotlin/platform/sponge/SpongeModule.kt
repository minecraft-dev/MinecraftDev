/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.spongePluginClassId
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class SpongeModule(facet: MinecraftFacet) : AbstractModule(facet) {

    override val moduleType = SpongeModuleType
    override val type = PlatformType.SPONGE
    override val icon = PlatformAssets.SPONGE_ICON

    override fun computeModIds(): List<String> {
        return findPluginsWithAnnotation(SpongeConstants.PLUGIN_ANNOTATION) + findPluginsWithAnnotation(SpongeConstants.JVM_PLUGIN_ANNOTATION)
    }

    private fun findPluginsWithAnnotation(annotation: String): List<String> {
        val pluginAnnotation =
            JavaPsiFacade.getInstance(project).findClass(annotation, GlobalSearchScope.allScope(project))
                ?: return emptyList()
        return AnnotatedElementsSearch.searchElements(pluginAnnotation, GlobalSearchScope.moduleScope(module), PsiClass::class.java)
            .mapNotNull { pluginClass ->
                pluginClass.spongePluginClassId()
            }
    }

    override val eventListenerGenSupport: EventListenerGenerationSupport = SpongeEventListenerGenerationSupport()

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        "org.spongepowered.api.event.Event" == eventClass.qualifiedName

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not an instance of org.spongepowered.api.event.Event\n" +
            "Compiling and running this listener may result in a runtime exception"

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { identifier.uastParent as? UClass ?: return false }

        if (psiClass == null || psiClass.javaPsi.hasModifier(JvmModifier.ABSTRACT)) {
            return false
        }

        return psiClass.findAnnotation(SpongeConstants.PLUGIN_ANNOTATION) != null ||
            psiClass.findAnnotation(SpongeConstants.JVM_PLUGIN_ANNOTATION) != null
    }

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? {
        val method = expression.findContainingMethod() ?: return null

        // Make sure this is an event listener method
        method.modifierList.findAnnotation(SpongeConstants.LISTENER_ANNOTATION) ?: return null

        val annotation = method.modifierList.findAnnotation(SpongeConstants.IS_CANCELLED_ANNOTATION)
        val isCancelled = if (annotation != null) {
            val value = annotation.findAttributeValue("value") ?: return null

            val text = value.text

            if (text.indexOf('.') == -1) {
                return null
            }

            when (text.substring(text.lastIndexOf('.') + 1)) {
                "TRUE" -> true
                "FALSE" -> false
                else -> return null
            }
        } else {
            false
        }

        val methodExpression = expression.methodExpression
        val qualifierExpression = methodExpression.qualifierExpression ?: return null
        if (standardSkip(method, qualifierExpression)) {
            return null
        }

        val resolve = methodExpression.resolve() as? PsiMethod ?: return null
        if (resolve.name != SpongeConstants.EVENT_ISCANCELLED_METHOD_NAME) {
            return null
        }

        val content = resolve.containingClass ?: return null
        if (!content.extendsOrImplements(SpongeConstants.CANCELLABLE)) {
            return null
        }

        return IsCancelled(
            errorString = "Cancellable.isCancelled() check is useless in a method not " +
                "annotated with @IsCancelled(Tristate.UNDEFINED)",
            fix = {
                expression.replace(
                    JavaPsiFacade.getElementFactory(project)
                        .createExpressionFromText(if (isCancelled) "true" else "false", expression),
                )
            },
        )
    }
}
