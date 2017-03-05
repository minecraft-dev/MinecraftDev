/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacetType.Companion.TYPE_ID
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.swing.Icon

class MinecraftFacet(module: Module, name: String, configuration: MinecraftFacetConfiguration, underlyingFacet: Facet<*>?) :
    Facet<MinecraftFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

    private val modules = ConcurrentHashMap<AbstractModuleType<*>, AbstractModule>()

    init {
        configuration.facet = this
    }

    override fun initFacet() {
        refresh()
    }

    fun refresh() {
        // Don't allow parent types with child types in auto detected set
        configuration.state.autoDetectTypes = PlatformType.removeParents(configuration.state.autoDetectTypes)

        val userEnabled = configuration.state.userChosenTypes.entries.stream()
            .filter { it.value }
            .map { it.key }

        val autoEnabled = configuration.state.autoDetectTypes.stream()
            .filter { configuration.state.userChosenTypes[it] != null }

        val allEnabled = Stream.concat(userEnabled, autoEnabled).collect(Collectors.toSet())

        // Remove modules that aren't registered anymore
        val toBeRemoved = modules.entries.stream()
            .filter { !allEnabled.contains(it.key.platformType) }
            .peek { it.value.dispose() }
            .map { it.key }
            .collect(Collectors.toSet())
        toBeRemoved.forEach { modules.remove(it) }

        // Add modules which are new
        allEnabled.stream()
            .map { it.type }
            .filter { !modules.containsKey(it) }
            .forEach { register(it) }
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
    fun <T : AbstractModule> getModuleOfType(type: AbstractModuleType<T>?): T? {
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
        @JvmField
        val ID = FacetTypeId<MinecraftFacet>(TYPE_ID)

        @JvmStatic
        val facetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MinecraftFacetType

        @JvmStatic
        fun getInstance(module: Module) = FacetManager.getInstance(module).getFacetByType(ID)

        @JvmStatic
        fun <T : AbstractModule> getInstance(module: Module, type: AbstractModuleType<T>): T? {
            val instance = getInstance(module) ?: return null
            return instance.getModuleOfType(type)
        }

        @JvmStatic
        fun <T : AbstractModule> getInstance(module: Module, vararg types: AbstractModuleType<*>): T? {
            val instance = getInstance(module) ?: return null
            @Suppress("UNCHECKED_CAST")
            return types.asSequence().mapNotNull { instance.getModuleOfType(it) }.firstOrNull() as? T
        }
    }
}
