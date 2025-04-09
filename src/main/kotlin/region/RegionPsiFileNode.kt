/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.region

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.util.containers.ContainerUtil

class RegionPsiFileNode(
    project: Project?,
    value: PsiFile,
    viewSettings: ViewSettings?,
) : PsiFileNode(project, value, viewSettings) {
    override fun getChildrenImpl(): MutableCollection<AbstractTreeNode<*>> {
        val rootDirectory = virtualFile?.let { RegionFileSystem.INSTANCE.getRootByLocal(it) }
        val project = project
        if (project != null && rootDirectory != null) {
            val psiRootDirectory = PsiManager.getInstance(project).findDirectory(rootDirectory)
            if (psiRootDirectory != null) {
                return psiRootDirectory
                    .children
                    .asSequence()
                    .mapNotNull { it as? PsiFile }
                    .map { PsiFileNode(it.project, it, settings) }
                    .toMutableList()
            }
        }

        return ContainerUtil.emptyList()
    }
}
