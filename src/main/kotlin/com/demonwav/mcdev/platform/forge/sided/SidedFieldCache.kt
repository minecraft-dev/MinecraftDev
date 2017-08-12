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
import com.intellij.psi.PsiField
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue

class SidedFieldCache(project: Project) : SidedCache<PsiField>(project) {

    override fun compute(field: PsiField): CachedValueProvider.Result<SideState>? {
        field.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)?.let { annotation ->
            val value = annotation.findAttributeValue("value") ?: return createResult(SideState(Side.INVALID, NULL_STRING, null))
            return createResult(SideState(SideOnlyUtil.getFromName(value.text), NULL_STRING, null))
        }

        // The field isn't annotated, so check for inheritors

        val classCache = SidedClassCache.getInstance(project)

        // First check the field's type
        val type = field.type as? PsiClassType
        type?.resolve()?.let { fieldClass ->
            val state = classCache.getSideState(fieldClass)
            if (state != null) {
                return createResult(SideState(state.side, fieldClass, SideReason.IS_OF_TYPE))
            }
        }

        // Now check the class that contains the field
        val containingClass = field.containingClass ?: return null

        val state = classCache.getSideState(containingClass)
        if (state != null) {
            return createResult(SideState(state.side, containingClass, SideReason.IS_CONTAINED_IN))
        }

        return null
    }

    override fun getSideState(psi: PsiField): SideState? {
        return CachedValuesManager.getManager(project).getParameterizedCachedValue(psi, KEY, this, true, psi)
    }

    companion object : SidedCacheCompanion<SidedFieldCache>(::SidedFieldCache) {
        private val KEY = Key.create<ParameterizedCachedValue<SideState, PsiField>>("SidedFieldCache")
    }
}
