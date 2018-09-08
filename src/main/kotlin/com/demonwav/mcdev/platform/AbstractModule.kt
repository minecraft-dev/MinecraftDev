/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.inspection.IsCancelled
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.annotations.Contract
import javax.swing.Icon

abstract class AbstractModule(protected val facet: MinecraftFacet) {

    val module: Module = facet.module
    val project = module.project

    abstract val moduleType: AbstractModuleType<*>

    abstract val type: PlatformType

    open val icon: Icon?
        get() = moduleType.icon

    /**
     * By default, this method is provided in the case that a specific platform has no
     * listener handling whatsoever, or simply accepts event listeners with random
     * classes. This is rather open ended. Primarily this should (platform dependent)
     * evaluate to the type (or multiple types) to determine whether the event listener
     * is not going to throw an error at runtime.

     * @param eventClass The PsiClass of the event listener argument
     * *
     * @param method The method of the event listener
     * *
     * @return True if the class is valid or ignored. Returning false may highlight the
     * *     method as an error and prevent compiling.
     */
    open fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = false

    open fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter does not extend the proper Event Class!"

    open fun doPreEventGenerate(psiClass: PsiClass, data: GenerationData?) {}

    open fun generateEventListenerMethod(containingClass: PsiClass,
                                         chosenClass: PsiClass,
                                         chosenName: String,
                                         data: GenerationData?): PsiMethod? = null

    @Contract(value = "null -> false", pure = true)
    open fun shouldShowPluginIcon(element: PsiElement?) = false

    @Contract(pure = true)
    open fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? {
        return null
    }

    open fun isStaticListenerSupported(method: PsiMethod) = false

    @Contract(pure = true)
    protected fun standardSkip(method: PsiMethod, qualifierExpression: PsiExpression): Boolean {
        if (qualifierExpression !is PsiReferenceExpression) {
            return false
        }

        val refResolve = qualifierExpression.resolve() ?: return false

        val parameters = method.parameterList.parameters
        return parameters.isNotEmpty() && refResolve != parameters[0]
    }

    open fun init() {}
    open fun dispose() {}
}
