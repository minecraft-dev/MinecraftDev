/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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
import com.demonwav.mcdev.platform.architectury.framework.ArchitecturyGradleData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom.ArchitecturyModel
import com.intellij.facet.FacetManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.search.GlobalSearchScopes
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.util.GradleUtil

@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
class MinecraftFacetDetector(
    private val project: Project,
    scope: CoroutineScope,
) {
    private val requests = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            requests
                .debounce(300.milliseconds)
                .collectLatest {
                    withBackgroundProgress(project, "Detecting Minecraft Frameworks", cancellable = true) {
                        val detections = smartReadAction(project) { detectModules() }
                        val needsReimport = withContext(Dispatchers.EDT) { applyDetections(detections) }
                        coroutineContext.ensureActive()
                        if (needsReimport) {
                            withContext(Dispatchers.EDT) {
                                ProjectReimporter.reimport(project)
                            }
                        }
                    }
                }
        }
    }

    fun schedule() {
        requests.tryEmit(Unit)
    }

    private fun detectModules(): List<ModuleDetection> = buildList {
        for (module in ModuleManager.getInstance(project).modules) {
            ProgressManager.checkCanceled()
            if (!module.isDisposed) {
                add(ModuleDetection(module, autoDetectTypes(module)))
            }
        }
    }

    private fun autoDetectTypes(module: Module): Set<PlatformType> {
        val platformTypes = mutableSetOf<PlatformType>()
        val libraryRoots = OrderEnumerator.orderEntries(module)
            .recursively()
            .librariesOnly()
            .classes()
            .roots

        if (libraryRoots.isNotEmpty()) {
            val libraryScope = GlobalSearchScopes.directoriesScope(module.project, true, *libraryRoots)
            for (detector in MinecraftLibraryDetector.EP_NAME.extensionList) {
                ProgressManager.checkCanceled()
                if (detector.isLibraryPresent(module.project, libraryScope)) {
                    platformTypes.add(detector.platformType)
                }
            }
        }

        OrderEnumerator.orderEntries(module)
            .recursively()
            .withoutLibraries()
            .withoutSdk()
            .forEachModule { dependencyModule ->
                if (dependencyModule.name.startsWith("SpongeAPI", ignoreCase = true)) {
                    val moduleManager = ModuleManager.getInstance(dependencyModule.project)
                    val groupPath = moduleManager.getModuleGrouper(null).getGroupPath(dependencyModule)
                    if (groupPath.isEmpty() || dependencyModule.name != groupPath.lastOrNull()) {
                        platformTypes.add(PlatformType.SPONGE)
                    }
                }
                true
            }

        val architecturyGradleData = GradleUtil.findGradleModuleData(module)?.children
            ?.find { it.key == ArchitecturyGradleData.KEY }?.data as? ArchitecturyGradleData
        if (architecturyGradleData?.moduleType == ArchitecturyModel.ModuleType.COMMON) {
            platformTypes.add(PlatformType.ARCHITECTURY)
            platformTypes.remove(PlatformType.FABRIC)
        }

        return platformTypes
    }

    private suspend fun applyDetections(detections: List<ModuleDetection>): Boolean {
        var needsReimport = false

        for ((module, platformTypes) in detections) {
            if (module.isDisposed) {
                continue
            }

            val facetManager = FacetManager.getInstance(module)
            val minecraftFacet = facetManager.getFacetByType(MinecraftFacet.ID)
            if (minecraftFacet == null) {
                if (platformTypes.isNotEmpty()) {
                    addFacet(module, platformTypes)
                }
                continue
            }

            if (platformTypes.isNotEmpty()) {
                val types = minecraftFacet.configuration.state.autoDetectTypes
                types.clear()
                types.addAll(platformTypes)

                if (minecraftFacet.configuration.state.forgePatcher) {
                    types.add(PlatformType.FORGE)
                    types.add(PlatformType.MCP)
                }

                minecraftFacet.refresh()
            }

            if (ProjectReimporter.needsReimport(minecraftFacet)) {
                needsReimport = true
            }
        }

        return needsReimport
    }

    private suspend fun addFacet(module: Module, platformTypes: Set<PlatformType>) {
        backgroundWriteAction {
            if (module.isDisposed) {
                return@backgroundWriteAction
            }

            val facetType = MinecraftFacet.facetTypeOrNull ?: return@backgroundWriteAction
            val facetManager = FacetManager.getInstance(module)
            val model = facetManager.createModifiableModel()
            if (model.getFacetByType(MinecraftFacet.ID) == null) {
                val configuration = MinecraftFacetConfiguration()
                configuration.state.autoDetectTypes.addAll(platformTypes)
                val facet = facetManager.createFacet(facetType, "Minecraft", configuration, null)
                model.addFacet(facet)
                model.commit()
            }
        }
    }

    private data class ModuleDetection(
        val module: Module,
        val platformTypes: Set<PlatformType>,
    )
}

class MinecraftFacetDetectorListener : ProjectActivity, ModuleRootListener {

    override suspend fun execute(project: Project) {
        project.service<MinecraftFacetDetector>().schedule()
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        if (event.isCausedByFileTypesChange) {
            return
        }

        val project = event.source as? Project ?: return
        project.service<MinecraftFacetDetector>().schedule()
    }
}
