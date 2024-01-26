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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager

/**
 * This class sets the icons for the modules in the project view.
 */
class MinecraftProjectViewNodeDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        if (!MinecraftSettings.instance.isShowProjectPlatformIcons) {
            return
        }

        if (node !is PsiDirectoryNode || !node.isValid) {
            return
        }

        val directory = node.value ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(directory) ?: return

        if (module.isDisposed || !module.isLoaded || module.project.isDisposed) {
            return
        }

        val rootManager = ModuleRootManager.getInstance(module)
        // Make sure there is at least a root to go off of
        if (node.virtualFile !in rootManager.contentRoots) {
            return
        }

        val facet = MinecraftFacet.getInstance(module) ?: return
        data.setIcon(facet.icon ?: return)
    }
}
