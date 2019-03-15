/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.ui.ColoredTreeCellRenderer

/**
 * This class sets the icons for the modules in the project view.
 */
class MinecraftProjectViewNodeDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        if (!MinecraftSettings.instance.isShowProjectPlatformIcons) {
            return
        }

        if (node !is PsiDirectoryNode) {
            return
        }

        val directory = node.value ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(directory) ?: return

        if (module.isDisposed || !module.isLoaded || module.project.isDisposed) {
            return
        }

        val rootManager = ModuleRootManager.getInstance(module)
        // Make sure there is at least a root to go off of
        if (rootManager.contentRoots.isEmpty()) {
            return
        }

        // Get the root and compare it to the node
        val root = rootManager.contentRoots[0]
        if (root != node.getVirtualFile()) {
            return
        }

        val facet = MinecraftFacet.getInstance(module) ?: return
        data.setIcon(facet.icon)
    }

    override fun decorate(node: PackageDependenciesNode, cellRenderer: ColoredTreeCellRenderer) {}
}
