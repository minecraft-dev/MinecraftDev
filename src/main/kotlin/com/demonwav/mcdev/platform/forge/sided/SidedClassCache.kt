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
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue

class SidedClassCache(project: Project) : SidedCache<PsiClass>(project) {

    override fun compute(psiClass: PsiClass): CachedValueProvider.Result<SideState>? {
        psiClass.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)?.let { annotation ->
            val value = annotation.findAttributeValue("value") ?: return createResult(SideState(Side.INVALID, NULL_STRING, null))
            return createResult(SideState(SideOnlyUtil.getFromName(value.text), NULL_STRING, null))
        }

        // No direct annotation, attempt to infer it's value
        // We check 2 types of hierarchy here
        //   1. Class java super class hierarchy
        //   2. Nested class hierarchy

        // Nested classes take precedence, so check it first
        // Note if it inherits side from both and they differ, an error will be presented
        val parent = psiClass.findContainingClass()
        parent?.let { par ->
            val state = getSideState(par)
            if (state != null) {
                return createResult(SideState(state.side, par, SideReason.IS_CONTAINED_IN))
            }
        }

        for (superClass in psiClass.supers) {
            // cyclic dependencies
            if (superClass == psiClass) {
                continue
            }

            // We only take the first side we find that returns a value
            val state = getSideState(superClass)
            if (state != null) {
                return createResult(SideState(state.side, superClass, SideReason.INHERITS_FROM))
            }
        }

        return null
    }

    override fun getSideState(psi: PsiClass): SideState? {
        return CachedValuesManager.getManager(project).getParameterizedCachedValue(psi, KEY, this, true, psi)
    }

    companion object : SidedCacheCompanion<SidedClassCache>(::SidedClassCache) {
        private val KEY = Key.create<ParameterizedCachedValue<SideState, PsiClass>>("SidedClassCache")
    }
}
