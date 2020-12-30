/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.generation.SpongeGenerationData
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class SpongeModule(facet: MinecraftFacet) : AbstractModule(facet) {

    override val moduleType = SpongeModuleType
    override val type = PlatformType.SPONGE
    override val icon = PlatformAssets.SPONGE_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        "org.spongepowered.api.event.Event" == eventClass.qualifiedName

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not an instance of org.spongepowered.api.event.Event\n" +
            "Compiling and running this listener may result in a runtime exception"

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

        val listenerAnnotation = modifierList.addAnnotation("org.spongepowered.api.event.Listener")

        val generationData = (data as SpongeGenerationData?)!!

        if (!generationData.isIgnoreCanceled) {
            val annotation = modifierList.addAnnotation("org.spongepowered.api.event.filter.IsCancelled")
            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText("org.spongepowered.api.util.Tristate.UNDEFINED", annotation)

            annotation.setDeclaredAttributeValue<PsiAnnotationMemberValue>("value", value)
        }

        if (generationData.eventOrder != "DEFAULT") {
            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(
                    "org.spongepowered.api.event.Order." + generationData.eventOrder,
                    listenerAnnotation
                )

            listenerAnnotation.setDeclaredAttributeValue<PsiAnnotationMemberValue>("order", value)
        }

        return method
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = identifier.uastParent as? UClass
            ?: return false

        return !psiClass.hasModifier(JvmModifier.ABSTRACT) &&
            psiClass.findAnnotation(SpongeConstants.PLUGIN_ANNOTATION) != null
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

            val sub = text.substring(text.lastIndexOf('.') + 1)
            when (sub) {
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
                        .createExpressionFromText(if (isCancelled) "true" else "false", expression)
                )
            }
        )
    }
}
