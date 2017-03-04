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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.platform.MinecraftFacetType.Companion.TYPE_ID
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.google.common.collect.Maps
import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

class MinecraftFacet(module: Module, name: String, configuration: MinecraftFacetConfiguration, underlyingFacet: Facet<*>?) :
    Facet<MinecraftFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

    val modules = ConcurrentHashMap<AbstractModuleType<*>, AbstractModule>()
    var buildSystem: BuildSystem? = null

    init {
        configuration.facet = this
    }

    override fun initFacet() {
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module)
        }
        buildSystem?.reImport(module)?.done {
            configuration.state.autoDetectTypes
                .filter { configuration.state.userChosenTypes[it] ?: true }
                .forEach { register(it.type) }

            configuration.state.userChosenTypes
                .filter { it.value }
                .filter { !configuration.state.autoDetectTypes.contains(it.key) }
                .forEach { k, _ -> register(k.type) }
        }
    }

    private fun register(type: AbstractModuleType<*>) {
        type.performCreationSettingSetup(module.project)
        modules.put(type, type.generateModule(module))
    }

    @Contract(pure = true)
    fun getModules(): Collection<AbstractModule> = modules.values
    @Contract(pure = true)
    fun getTypes(): Collection<AbstractModuleType<*>> = modules.keys

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

    @Contract(pure = true)
    fun writeErrorMessageForEvent(eventClass: PsiClass, method: PsiMethod): String? {
        return doIfGood(method) {
            it.writeErrorMessageForEventParameter(eventClass, method)
        }
    }

    @Contract(pure = true)
    fun isStaticListenerSupported(eventClass: PsiClass, method: PsiMethod): Boolean {
        return doIfGood(method) {
            it.isStaticListenerSupported(eventClass, method)
        } ?: false
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

    fun addModuleType(moduleTypeName: String) {
        val type = PlatformType.getByName(moduleTypeName)
        if (type != null && !modules.containsKey(type)) {
            modules[type] = type.generateModule(module)
        }
        ProjectView.getInstance(module.project).refresh()
    }

    @Contract(pure = true)
    fun isEventGenAvailable() = modules.keys.any { it.isEventGenAvailable }

    @Contract(pure = true)
    fun shouldShowPluginIcon(element: PsiElement?) = modules.values.any { it.shouldShowPluginIcon(element) }

    @Contract(pure = true)
    fun getIcon(): Icon? {
        if (modules.keys.count { it.hasIcon() } == 1) {
            return modules.values.iterator().next().icon
        } else if (
            modules.keys.count { it.hasIcon() } == 2 &&
            modules.containsKey(SpongeModuleType) &&
            modules.containsKey(ForgeModuleType)
        ) {
            return PlatformAssets.SPONGE_FORGE_ICON
        } else {
            return PlatformAssets.MINECRAFT_ICON
        }
    }

    companion object {
        private val instanceMap = Maps.newHashMap<Module, MinecraftFacet>()

        @JvmField
        val ID = FacetTypeId<MinecraftFacet>(TYPE_ID)

        @JvmStatic
        val facetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MinecraftFacetType

        @JvmStatic
        fun getInstance(module: Module): MinecraftFacet? {
            val testInstance = instanceMap[module]
            if (testInstance != null) {
                return testInstance
            }

            var facet = FacetManager.getInstance(module).getFacetByType(MinecraftFacet.ID)
            if (facet != null) {
                instanceMap[module] = facet
                return facet
            }

            val paths = ModuleManager.getInstance(module.project).getModuleGroupPath(module)
            if (paths == null || paths.isEmpty()) {
                return null
            }

            val parentModule = ApplicationManager.getApplication().acquireReadActionLock().use {
                ModuleManager.getInstance(module.project).findModuleByName(paths.last())
            } ?: return null

            facet = FacetManager.getInstance(parentModule).getFacetByType(ID)

            if (facet != null) {
                instanceMap[parentModule] = facet
                return facet
            }
            return null
        }

        @JvmStatic
        fun <T : AbstractModule> getInstance(module: Module, type: AbstractModuleType<T>): T? {
            val instance = getInstance(module) ?: return null
            return instance.getModuleType(type)
        }

        @JvmStatic
        fun <T : AbstractModule> getInstance(module: Module, vararg types: AbstractModuleType<*>): T? {
            val instance = getInstance(module) ?: return null
            @Suppress("UNCHECKED_CAST")
            return types.asSequence().mapNotNull { instance.getModuleType(it) }.firstOrNull() as? T
        }

        @Contract(pure = true)
        fun searchAllModulesForFile(project: Project, path: String, type: SourceType): VirtualFile? {
            val modules = ModuleManager.getInstance(project).modules
            return modules.asSequence()
                .mapNotNull(this::getInstance)
                .filter { it.buildSystem != null }
                .mapNotNull { it.buildSystem!!.findFile(path, type) }
                .firstOrNull()
        }
    }
}
