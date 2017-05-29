/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.AbstractProjectComponent
import com.demonwav.mcdev.util.runInlineReadAction
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class SideOnlyProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        val facets = ProjectFacetManager.getInstance(project).getFacets(MinecraftFacet.ID)
        if (facets.none { f -> f.isOfType(ForgeModuleType) }) {
            return
        }

        StartupManager.getInstance(project).registerPostStartupActivity {
            DumbService.getInstance(project).smartInvokeLater {
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Indexing @SidedProxy", true, null) {
                    override fun run(indicator: ProgressIndicator) {
                        runInlineReadAction {
                            indicator.isIndeterminate = true
                            val scope = GlobalSearchScope.projectScope(myProject)
                            val sidedProxy = JavaPsiFacade.getInstance(myProject).findClass(ForgeConstants.SIDED_PROXY_ANNOTATION, scope) ?: return
                            val annotatedFields = AnnotatedElementsSearch.searchPsiFields(sidedProxy, scope).findAll()

                            indicator.isIndeterminate = false
                            var index = 0.0

                            for (field in annotatedFields) {
                                SidedProxyAnnotator.check(field)
                                index++
                                indicator.fraction = index / annotatedFields.size
                            }
                        }
                    }
                })
            }
        }
    }
}
