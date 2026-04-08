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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.inspection.IsCancelled
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import javax.swing.Icon

abstract class AbstractModule(protected val facet: MinecraftFacet) {

    val module: Module = facet.module
    val project = module.project

    abstract val moduleType: AbstractModuleType<*>

    abstract val type: PlatformType

    open val icon: Icon?
        get() = moduleType.icon

    protected open fun computeModIds(): List<String> = emptyList()

    private val modIdsCacheKey = Key.create<CachedValue<List<String>>>("modIds for ${javaClass.simpleName}")

    /**
     * Returns a list of mod IDs for this module. Must be called in a read action!
     */
    val modIds: List<String>
        get() {
            val provider = CachedValueProvider {
                CachedValueProvider.Result(computeModIds(), PsiModificationTracker.MODIFICATION_COUNT)
            }
            return CachedValuesManager.getManager(project).getCachedValue(module, modIdsCacheKey, provider, false)
        }

    open val eventListenerGenSupport: EventListenerGenerationSupport? = null

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

    open fun shouldShowPluginIcon(element: PsiElement?) = false

    open fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? {
        return null
    }

    open fun isStaticListenerSupported(method: PsiMethod) = false

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
    open fun refresh() {}
}
