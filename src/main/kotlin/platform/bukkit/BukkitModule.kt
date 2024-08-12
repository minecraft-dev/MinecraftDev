/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.platform.bukkit.util.PaperConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.createVoidMethodWithParameterType
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.util.application
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class BukkitModule<out T : AbstractModuleType<*>>(facet: MinecraftFacet, type: T) : AbstractModule(facet) {

    // Light test cases only support a single source content root, so we mix sources and resources under unit test mode
    private val pluginYmlSourceType = if (application.isUnitTestMode) SourceType.SOURCE else SourceType.RESOURCE

    var pluginYml by nullable {
        if (moduleType is PaperModuleType) {
            val paperPlugin = facet.findFile("paper-plugin.yml", pluginYmlSourceType)
            if (paperPlugin != null) {
                return@nullable paperPlugin
            }
        }

        facet.findFile("plugin.yml", pluginYmlSourceType)
    }
        private set

    override val type: PlatformType = type.platformType

    override val moduleType: T = type

    override val eventListenerGenSupport: EventListenerGenerationSupport? = BukkitEventListenerGenerationSupport()

    private val pluginParentClasses = listOf(
        BukkitConstants.PLUGIN,
        PaperConstants.PLUGIN_BOOTSTRAP,
        PaperConstants.PLUGIN_LOADER,
    )

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        BukkitConstants.EVENT_CLASS == eventClass.qualifiedName

    override fun isStaticListenerSupported(method: PsiMethod) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not a subclass of org.bukkit.event.Event\n" +
            "Compiling and running this listener may result in a runtime exception"

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? {
        val method = expression.findContainingMethod() ?: return null

        val annotation = method.modifierList.findAnnotation(BukkitConstants.HANDLER_ANNOTATION) ?: return null

        // We are in an event method
        val value = annotation.findAttributeValue("ignoreCancelled") as? PsiLiteralExpression ?: return null
        if (value.value !is Boolean) {
            return null
        }

        val ignoreCancelled = (value.value as Boolean?)!!

        // If we aren't ignoring cancelled then any check for event being cancelled is valid
        if (!ignoreCancelled) {
            return null
        }

        val methodExpression = expression.methodExpression
        val qualifierExpression = methodExpression.qualifierExpression
        val resolve = methodExpression.resolve()

        if (qualifierExpression == null) {
            return null
        }
        if (resolve == null) {
            return null
        }

        if (standardSkip(method, qualifierExpression)) {
            return null
        }

        val context = resolve.context as? PsiClass ?: return null

        if (!context.extendsOrImplements(BukkitConstants.CANCELLABLE_CLASS)) {
            return null
        }

        if (resolve !is PsiMethod) {
            return null
        }

        if (resolve.name != BukkitConstants.EVENT_ISCANCELLED_METHOD_NAME) {
            return null
        }

        return IsCancelled(
            errorString = "Cancellable.isCancelled() check is useless in a method annotated with ignoreCancelled=true.",
            fix = {
                expression.replace(
                    JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                        "false",
                        expression,
                    ),
                )
            },
        )
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { (identifier.uastParent as? UClass)?.javaPsi }
            ?: return false

        if (psiClass.hasModifier(JvmModifier.ABSTRACT)) {
            return false
        }

        val project = element.project
        for (className in pluginParentClasses) {
            val pluginInterface = JavaPsiFacade.getInstance(project)
                .findClass(className, module.getModuleWithDependenciesAndLibrariesScope(false))
                ?: continue

            if (psiClass.isInheritor(pluginInterface, true)) {
                return true
            }
        }

        return false
    }

    override fun dispose() {
        super.dispose()

        pluginYml = null
    }

    companion object {
        fun generateBukkitStyleEventListenerMethod(
            chosenClass: PsiClass,
            chosenName: String,
            project: Project,
            annotationName: String,
            setIgnoreCancelled: Boolean,
        ): PsiMethod? {
            val newMethod = createVoidMethodWithParameterType(project, chosenName, chosenClass) ?: return null
            val modifierList = newMethod.modifierList
            val annotation = modifierList.addAnnotation(annotationName)

            if (setIgnoreCancelled) {
                val value = JavaPsiFacade.getElementFactory(project).createExpressionFromText("true", annotation)
                annotation.setDeclaredAttributeValue("ignoreCancelled", value)
            }

            return newMethod
        }
    }
}
