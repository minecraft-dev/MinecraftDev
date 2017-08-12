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

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.ParameterizedCachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import java.util.concurrent.ConcurrentHashMap

abstract class SidedCache<T> protected constructor(protected val project: Project) : ParameterizedCachedValueProvider<SideState, T> {

    abstract fun getSideState(psi: T): SideState?

    fun createResult(state: SideState): CachedValueProvider.Result<SideState> {
        return CachedValueProvider.Result.createSingleDependency(state, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT)
    }

    abstract class SidedCacheCompanion<out R : SidedCache<*>>(private val constructor: (Project) -> R) {
        protected val NULL_STRING: String? = null
        private val map = ConcurrentHashMap<String, R>()

        fun getInstance(project: Project) = map.computeIfAbsent(project.locationHash) { constructor(project) }
    }
}
