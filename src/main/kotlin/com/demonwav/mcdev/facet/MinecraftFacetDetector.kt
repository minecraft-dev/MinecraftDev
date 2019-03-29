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

import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND
import com.demonwav.mcdev.util.AbstractProjectComponent
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.ProjectTopics
import com.intellij.facet.FacetManager
import com.intellij.facet.impl.ui.libraries.LibrariesValidatorContextImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager
import com.intellij.openapi.startup.StartupManager

class MinecraftFacetDetector(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        val manager = StartupManager.getInstance(project)
        val connection = project.messageBus.connect()

        manager.registerStartupActivity {
            MinecraftModuleRootListener.doCheck(project)
        }

        // Register a module root listener to check when things change
        manager.registerPostStartupActivity {
            connection.subscribe(ProjectTopics.PROJECT_ROOTS, MinecraftModuleRootListener)
        }
    }

    private object MinecraftModuleRootListener : ModuleRootListener {
        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByFileTypesChange) {
                return
            }

            val project = event.source as? Project ?: return
            doCheck(project)
        }

        fun doCheck(project: Project) {
            val moduleManager = ModuleManager.getInstance(project)
            for (module in moduleManager.modules) {
                val facetManager = FacetManager.getInstance(module)
                val minecraftFacet = facetManager.getFacetByType(MinecraftFacet.ID)

                if (minecraftFacet == null) {
                    checkNoFacet(module)
                } else {
                    checkExistingFacet(module, minecraftFacet)
                }
            }
        }

        private fun checkNoFacet(module: Module) {
            val platforms = autoDetectTypes(module).ifEmpty { return }

            val facetManager = FacetManager.getInstance(module)
            val configuration = MinecraftFacetConfiguration()
            configuration.state.autoDetectTypes.addAll(platforms)

            val facet = facetManager.createFacet(MinecraftFacet.facetType, "Minecraft", configuration, null)
            runWriteTaskLater {
                // Only add the new facet if there isn't a Minecraft facet already - double check here since this
                // task may run much later
                if (module.isDisposed || facet.isDisposed) {
                    // Module may be disposed before we run
                    return@runWriteTaskLater
                }
                if (facetManager.getFacetByType(MinecraftFacet.ID) == null) {
                    val model = facetManager.createModifiableModel()
                    model.addFacet(facet)
                    model.commit()
                }
            }
        }

        private fun checkExistingFacet(module: Module, facet: MinecraftFacet) {
            val platforms = autoDetectTypes(module).ifEmpty { return }

            val types = facet.configuration.state.autoDetectTypes
            types.clear()
            types.addAll(platforms)

            if (facet.configuration.state.forgePatcher) {
                // make sure Forge and MCP are present
                types.add(PlatformType.FORGE)
                types.add(PlatformType.MCP)
            }

            facet.refresh()
        }

        private fun autoDetectTypes(module: Module): Set<PlatformType> {
            val presentationManager = LibraryPresentationManager.getInstance()
            val context = LibrariesValidatorContextImpl(module)

            val platformKinds = mutableSetOf<LibraryKind>()
            context.rootModel
                .orderEntries()
                .using(context.modulesProvider)
                .recursively()
                .librariesOnly()
                .forEachLibrary forEach@ { library ->
                    MINECRAFT_LIBRARY_KINDS.forEach { kind ->
                        if (presentationManager.isLibraryOfKind(library, context.librariesContainer, setOf(kind))) {
                            platformKinds.add(kind)
                        }
                    }
                    return@forEach true
                }

            context.rootModel
                .orderEntries()
                .using(context.modulesProvider)
                .recursively()
                .withoutLibraries()
                .withoutSdk()
                .forEachModule forEach@ { m ->
                    if (m.name.startsWith("SpongeAPI")) {
                        // We don't want want to add parent modules in module groups
                        val moduleManager = ModuleManager.getInstance(m.project)
                        val groupPath = moduleManager.getModuleGroupPath(m)
                        if (groupPath == null) {
                            platformKinds.add(SPONGE_LIBRARY_KIND)
                            return@forEach true
                        }

                        val name = groupPath.lastOrNull() ?: return@forEach true
                        if (m.name == name) {
                            return@forEach true
                        }

                        platformKinds.add(SPONGE_LIBRARY_KIND)
                    }
                    return@forEach true
                }

            return platformKinds.mapNotNull { kind -> PlatformType.fromLibraryKind(kind) }.toSet()
        }
    }
}
