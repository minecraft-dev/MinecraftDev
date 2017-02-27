/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.intellij.facet.Facet
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract
import java.util.concurrent.ConcurrentHashMap

class MinecraftFacet(module: Module, name: String, configuration: MinecraftFacetConfiguration) :
    Facet<MinecraftFacetConfiguration>(facetType, module, name, configuration, null) {

    val modules = ConcurrentHashMap<AbstractModuleType<*>, AbstractModule>()
    val buildSystem
        get() = configuration.state!!.buildSystem

    init {
        configuration.facet = this
    }

    override fun initFacet() {
        // TODO
    }

    @Contract(pure = true) fun getModules(): Collection<AbstractModule> = modules.values
    @Contract(pure = true) fun getTypes(): Collection<AbstractModuleType<*>> = modules.keys

    @Contract(value = "null -> false", pure = true)
    fun isOfType(type: AbstractModuleType<*>?) = modules.containsKey(type)

    @Contract(value = "null -> null", pure = true)
    fun <T : AbstractModule> getModuleType(type: AbstractModuleType<T>?): T? {
        @Suppress("UNCHECKED_CAST")
        return modules[type as AbstractModuleType<*>] as? T
    }

    @Contract(value = "null -> false", pure = true)
    fun isEventClassValidForModule(eventClass: PsiClass?): Boolean {
        if (eventClass == null) {
            return false
        }

        return modules.values.any { it.isEventClassValid(eventClass, null) }
    }

    @Contract(pure = true)
    fun isEventClassValid(eventClass: PsiClass, method: PsiMethod): Boolean {
        return doIfGood(method) {
            it.isEventClassValid(eventClass, method)
        } ?: false
    }

    fun writeErrorMessageForEvent(eventClass: PsiClass, method: PsiMethod): String? {
        return doIfGood(method) {
            it.writeErrorMessageForEventParameter(eventClass, method)
        }
    }

    fun isStaticListenerSupported(eventClass: PsiClass, method: PsiMethod): Boolean {
        return doIfGood(method) {
            it.isStaticListenerSupported(eventClass, method)
        } ?: false
    }

    fun addModuleType(moduleTypeName: String) {
        val type = PlatformType.getByName(moduleTypeName)
        if (type != null && !modules.containsKey(type)) {
            modules[type] = type.generateModule(module)
        }
        ProjectView.getInstance(module.project).refresh()
    }

    private inline fun <T> doIfGood(method: PsiMethod, action: (AbstractModule) -> T): T? {
        for (abstractModule in modules.values) {
            val good = abstractModule.moduleType.listenerAnnotations.any {
                method.modifierList.findAnnotation(it) != null
            }

            if (good) {
                return action(abstractModule)
            }
        }
        return null
    }

    companion object {
        val ID = FacetTypeId<MinecraftFacet>("minecraft")
        val facetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MinecraftFacetType
    }
}
