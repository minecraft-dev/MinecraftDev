/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.architectury.framework.ARCHITECTURY_LIBRARY_KIND
import com.demonwav.mcdev.platform.architectury.framework.ArchitecturyGradleData
import com.demonwav.mcdev.platform.fabric.framework.FABRIC_LIBRARY_KIND
import com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom.ArchitecturyModel
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.facet.FacetManager
import com.intellij.facet.impl.ui.libraries.LibrariesValidatorContextImpl
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryDetectionManager
import com.intellij.openapi.roots.libraries.LibraryDetectionManager.LibraryPropertiesProcessor
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Key
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.forEachWithProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.plugins.gradle.util.GradleUtil

class MinecraftFacetDetector : ProjectActivity {
    companion object {
        private val libraryVersionsKey = Key<MutableMap<LibraryKind, String>>("mcdev.libraryVersions")

        fun getLibraryVersions(module: Module): Map<LibraryKind, String> {
            return module.getUserData(libraryVersionsKey) ?: emptyMap()
        }
    }

    override suspend fun execute(project: Project) {
        val detectorService = project.service<FacetDetectorScopeProvider>()
        detectorService.currentJob?.cancelAndJoin()
        withBackgroundProgress(project, "Detecting Minecraft Frameworks", cancellable = false) {
            detectorService.currentJob = coroutineContext.job
            MinecraftModuleRootListener.doCheck(project)
        }
    }

    @Service(Service.Level.PROJECT)
    private class FacetDetectorScopeProvider(val scope: CoroutineScope) {
        var currentJob: Job? = null
    }

    private object MinecraftModuleRootListener : ModuleRootListener {
        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByFileTypesChange) {
                return
            }

            val project = event.source as? Project ?: return
            val detectorService = project.service<FacetDetectorScopeProvider>()
            detectorService.scope.launch {
                detectorService.currentJob?.cancelAndJoin()
                withBackgroundProgress(project, "Detecting Minecraft Frameworks", cancellable = false) {
                    detectorService.currentJob = coroutineContext.job
                    doCheck(project)
                }
            }
        }

        suspend fun doCheck(project: Project) {
            val moduleManager = ModuleManager.getInstance(project)

            var needsReimport = false

            moduleManager.modules.asList().forEachWithProgress { module ->
                val facetManager = FacetManager.getInstance(module)
                val minecraftFacet = facetManager.getFacetByType(MinecraftFacet.ID)

                if (minecraftFacet == null) {
                    checkNoFacet(module)
                } else {
                    checkExistingFacet(module, minecraftFacet)
                    if (ProjectReimporter.needsReimport(minecraftFacet)) {
                        needsReimport = true
                    }
                }
            }

            if (needsReimport) {
                project.service<FacetDetectorScopeProvider>().scope.launch(Dispatchers.EDT) {
                    ProjectReimporter.reimport(project)
                }
            }
        }

        private fun checkNoFacet(module: Module) {
            val platforms = autoDetectTypes(module).ifEmpty { return }

            runWriteTaskLater {
                // Only add the new facet if there isn't a Minecraft facet already - double check here since this
                // task may run much later
                if (module.isDisposed) {
                    // Module may be disposed before we run
                    return@runWriteTaskLater
                }

                val facetType = MinecraftFacet.facetTypeOrNull
                    ?: return@runWriteTaskLater

                val facetManager = FacetManager.getInstance(module)
                val model = facetManager.createModifiableModel()
                if (model.getFacetByType(MinecraftFacet.ID) == null) {
                    val configuration = MinecraftFacetConfiguration()
                    configuration.state.autoDetectTypes.addAll(platforms)
                    val facet = facetManager.createFacet(facetType, "Minecraft", configuration, null)
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
            val libraryVersions = module.getUserData(libraryVersionsKey)
                ?: mutableMapOf<LibraryKind, String>().also { module.putUserData(libraryVersionsKey, it) }
            libraryVersions.clear()

            val context = LibrariesValidatorContextImpl(module)

            val platformKinds = mutableSetOf<LibraryKind>()
            context.rootModel
                .orderEntries()
                .using(context.modulesProvider)
                .recursively()
                .librariesOnly()
                .forEachLibrary forEach@{ library ->
                    processLibraryMinecraftPlatformKinds(library, context.librariesContainer) { kind, version ->
                        platformKinds.add(kind)
                        if (version != null) {
                            libraryVersions[kind] = version
                        }
                        true
                    }
                    return@forEach true
                }

            context.rootModel
                .orderEntries()
                .using(context.modulesProvider)
                .recursively()
                .withoutLibraries()
                .withoutSdk()
                .forEachModule forEach@{ m ->
                    if (m.name.startsWith("SpongeAPI", ignoreCase = true)) {
                        // We don't want want to add parent modules in module groups
                        val moduleManager = ModuleManager.getInstance(m.project)
                        val groupPath = moduleManager.getModuleGrouper(null).getGroupPath(m)
                        if (groupPath.isEmpty()) {
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

            val architecturyGradleData = GradleUtil.findGradleModuleData(module)?.children
                ?.find { it.key == ArchitecturyGradleData.KEY }?.data as? ArchitecturyGradleData
            if (architecturyGradleData?.moduleType == ArchitecturyModel.ModuleType.COMMON) {
                platformKinds.add(ARCHITECTURY_LIBRARY_KIND)
                platformKinds.removeIf { it == FABRIC_LIBRARY_KIND }
            }
            return platformKinds.mapNotNullTo(mutableSetOf()) { kind -> PlatformType.fromLibraryKind(kind) }
        }

        private fun processLibraryMinecraftPlatformKinds(
            library: Library,
            container: LibrariesContainer,
            action: (kind: LibraryKind, version: String?) -> Boolean
        ): Boolean {
            val libraryFiles = container.getLibraryFiles(library, OrderRootType.CLASSES)
            val propertiesProcessor = object : LibraryPropertiesProcessor {
                override fun <P : LibraryProperties<*>> processProperties(kind: LibraryKind, properties: P): Boolean {
                    if (kind in MINECRAFT_LIBRARY_KINDS) {
                        val version = (properties as? LibraryVersionProperties)?.versionString
                        return action(kind, version)
                    }
                    return true
                }
            }
            return LibraryDetectionManager.getInstance().processProperties(libraryFiles.asList(), propertiesProcessor)
        }
    }
}
