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
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.ide.projectView.ProjectView
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
    val buildSystem by lazy {
        BuildSystem.getInstance(module)
    }

    init {
        configuration.facet = this
    }

    override fun initFacet() {
        buildSystem?.apply {
            reImport(module).done {
                val types = configuration.state.types
                types.forEach { type -> register(type.type) }
            }
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
        @JvmField
        val ID = FacetTypeId<MinecraftFacet>("minecraft")
        @JvmStatic
        val facetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MinecraftFacetType

        @JvmStatic
        fun getInstance(module: Module) = FacetManager.getInstance(module).getFacetByType(MinecraftFacet.ID)
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
