/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.sided

import com.demonwav.mcdev.platform.forge.inspections.sideonly.SideOnlyUtil
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue

class SidedMethodCache(project: Project) : SidedCache<PsiMethod>(project) {

    override fun compute(method: PsiMethod): CachedValueProvider.Result<SideState>? {
        method.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)?.let { annotation ->
            val value = annotation.findAttributeValue("value") ?: return createResult(SideState(Side.INVALID, NULL_STRING, null))
            return createResult(SideState(SideOnlyUtil.getFromName(value.text), NULL_STRING, null))
        }

        // The method isn't annotated, so check for inheritors

        val classCache = SidedClassCache.getInstance(project)

        // First check the class the method's in
        method.containingClass?.let { containingClass ->
            val state = classCache.getSideState(containingClass)
            if (state != null) {
                return createResult(SideState(state.side, containingClass, SideReason.IS_CONTAINED_IN))
            }
        }

        // Check if this method overrides an existing method
        // If it does, then use that value
        for (superMethod in method.findSuperMethods()) {
            // Take the first found
            val state = getSideState(superMethod)
            if (state != null) {
                return createResult(SideState(state.side, superMethod, SideReason.OVERRIDES))
            }
        }

        // Check return type
        (method.returnType as? PsiClassType)?.resolve()?.let { returnClass ->
            val state = classCache.getSideState(returnClass)
            if (state != null) {
                return createResult(SideState(state.side, returnClass, SideReason.RETURNS_VALUE_OF))
            }
        }

        // Check parameter types
        for (parameter in method.parameterList.parameters) {
            (parameter.type as? PsiClassType)?.resolve()?.let { parameterClass ->
                val state = classCache.getSideState(parameterClass)
                if (state != null) {
                    return createResult(SideState(state.side, parameterClass, SideReason.CONTAINS_PARAMETER_OF))
                }
            }
        }

        return null
    }

    override fun getSideState(psi: PsiMethod): SideState? {
        return CachedValuesManager.getManager(project).getParameterizedCachedValue(psi, KEY, this, true, psi)
    }

    companion object : SidedCacheCompanion<SidedMethodCache>(::SidedMethodCache) {
        private val KEY = Key.create<ParameterizedCachedValue<SideState, PsiMethod>>("SidedMethodCache")
    }
}
