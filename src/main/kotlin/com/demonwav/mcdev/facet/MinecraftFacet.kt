/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacetType.Companion.TYPE_ID
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.util.containsAllKeys
import com.demonwav.mcdev.util.filterNotNull
import com.demonwav.mcdev.util.mapFirstNotNull
import com.google.common.collect.HashMultimap
import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

class MinecraftFacet(module: Module, name: String, configuration: MinecraftFacetConfiguration, underlyingFacet: Facet<*>?) :
    Facet<MinecraftFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

    private val moduleMap = ConcurrentHashMap<AbstractModuleType<*>, AbstractModule>()
    private val roots: HashMultimap<SourceType, VirtualFile?> = HashMultimap.create()

    init {
        configuration.facet = this
    }

    override fun initFacet() {
        refresh()
    }

    override fun disposeFacet() {
        moduleMap.forEach { (_, m) ->
            m.dispose()
        }
        moduleMap.clear()
        roots.clear()
    }

    fun refresh() {
        // Don't allow parent types with child types in auto detected set
        val allEnabled = configuration.state.run {
            autoDetectTypes = PlatformType.removeParents(autoDetectTypes)

            val userEnabled = userChosenTypes.entries.asSequence()
                .filter { it.value }
                .map { it.key }

            val autoEnabled = autoDetectTypes.asSequence()
                .filter { userChosenTypes[it] == null }

            userEnabled + autoEnabled
        }

        // Remove modules that aren't registered anymore
        val toBeRemoved = moduleMap.entries.asSequence()
            .filter { !allEnabled.contains(it.key.platformType) }
            .onEach { it.value.dispose() }
            .map { it.key }
            .toHashSet() // CME defense
        toBeRemoved.forEach { moduleMap.remove(it) }

        // Do this before we register the new modules
        updateRoots()

        // Add modules which are new
        val newlyEnabled = mutableListOf<AbstractModule>()
        allEnabled
            .map { it.type }
            .filter { !moduleMap.containsKey(it) }
            .forEach {
                newlyEnabled += register(it)
            }

        newlyEnabled.forEach(AbstractModule::init)

        ProjectView.getInstance(module.project).refresh()
    }

    private fun updateRoots() {
        roots.clear()
        val rootManager = ModuleRootManager.getInstance(module)

        rootManager.contentEntries.asSequence()
            .flatMap { entry -> entry.sourceFolders.asSequence() }
            .filterNotNull { it.file }
            .forEach {
                when (it.rootType) {
                    JavaSourceRootType.SOURCE -> roots.put(SourceType.SOURCE, it.file)
                    JavaSourceRootType.TEST_SOURCE -> roots.put(SourceType.TEST_SOURCE, it.file)
                    JavaResourceRootType.RESOURCE -> roots.put(SourceType.RESOURCE, it.file)
                    JavaResourceRootType.TEST_RESOURCE -> roots.put(SourceType.TEST_RESOURCE, it.file)
                }
            }
    }

    private fun register(type: AbstractModuleType<*>): AbstractModule {
        type.performCreationSettingSetup(module.project)
        val module = type.generateModule(this)
        moduleMap[type] = module
        return module
    }

    val modules get() = moduleMap.values
    val types get() = moduleMap.keys

    @Contract(pure = true)
    fun isOfType(type: AbstractModuleType<*>) = moduleMap.containsKey(type)

    @Contract(pure = true)
    fun <T : AbstractModule> getModuleOfType(type: AbstractModuleType<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return moduleMap[type] as? T
    }

    @Contract(value = "null -> false", pure = true)
    fun isEventClassValidForModule(eventClass: PsiClass?) =
        eventClass != null && moduleMap.values.any { it.isEventClassValid(eventClass, null) }

    @Contract(pure = true)
    fun isEventClassValid(eventClass: PsiClass, method: PsiMethod): Boolean {
        return doIfGood(method) {
            it.isEventClassValid(eventClass, method)
        } == true
    }

    @Contract(pure = true)
    fun writeErrorMessageForEvent(eventClass: PsiClass, method: PsiMethod): String? {
        return doIfGood(method) {
            it.writeErrorMessageForEventParameter(eventClass, method)
        }
    }

    @Contract(pure = true)
    fun isStaticListenerSupported(method: PsiMethod): Boolean {
        return doIfGood(method) {
            it.isStaticListenerSupported(method)
        } == true
    }

    @Contract(pure = true)
    fun suppressStaticListener(method: PsiMethod): Boolean {
        return doIfGood(method) {
            !it.isStaticListenerSupported(method)
        } == true
    }

    private inline fun <T> doIfGood(method: PsiMethod, action: (AbstractModule) -> T): T? {
        for (abstractModule in moduleMap.values) {
            val good = abstractModule.moduleType.listenerAnnotations.any {
                method.modifierList.findAnnotation(it) != null
            }

            if (good) {
                return action(abstractModule)
            }
        }
        return null
    }

    val isEventGenAvailable get() = moduleMap.keys.any { it.isEventGenAvailable }

    @Contract(pure = true)
    fun shouldShowPluginIcon(element: PsiElement?) = moduleMap.values.any { it.shouldShowPluginIcon(element) }

    val icon: Icon?
        get() {
            val iconCount = moduleMap.keys.count { it.hasIcon }
            return when {
                iconCount == 0 -> null
                iconCount == 1 -> moduleMap.keys.firstOrNull { it.hasIcon }?.icon
                iconCount == 2 && moduleMap.containsAllKeys(SpongeModuleType, ForgeModuleType) -> PlatformAssets.SPONGE_FORGE_ICON
                moduleMap.size > 0 -> PlatformAssets.MINECRAFT_ICON
                else -> null
            }
        }

    fun findFile(path: String, type: SourceType): VirtualFile? {
        val roots = roots[type]
        for (root in roots) {
            return root?.findFileByRelativePath(path) ?: continue
        }
        return null
    }

    companion object {
        val ID = FacetTypeId<MinecraftFacet>(TYPE_ID)

        val facetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MinecraftFacetType

        fun getInstance(module: Module) = FacetManager.getInstance(module).getFacetByType(ID)

        fun getChildInstances(module: Module) = runReadAction run@{
            val instance = getInstance(module)
            if (instance != null) {
                return@run setOf(instance)
            }

            val manager = ModuleManager.getInstance(module.project)
            val result = mutableSetOf<MinecraftFacet>()

            for (m in manager.modules) {
                val path = manager.getModuleGroupPath(m) ?: continue
                val namedModule = manager.findModuleByName(path.last()) ?: continue

                if (namedModule != module) {
                    continue
                }

                val facet = getInstance(m) ?: continue
                result.add(facet)
            }
            return@run result
        }


        fun <T : AbstractModule> getInstance(module: Module, type: AbstractModuleType<T>) = getInstance(module)?.getModuleOfType(type)

        fun <T : AbstractModule> getInstance(module: Module, vararg types: AbstractModuleType<T>): T? {
            val instance = getInstance(module) ?: return null
            return types.mapFirstNotNull { instance.getModuleOfType(it) }
        }
    }
}
