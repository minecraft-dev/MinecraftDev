/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.facet.MinecraftFacet;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.ColoredTreeCellRenderer;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * This class sets the icons for the modules in the project view.
 */
public class MinecraftProjectViewNodeDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (!MinecraftSettings.getInstance().isShowProjectPlatformIcons()) {
            return;
        }

        final Project project = node.getProject();
        if (project == null) {
            return;
        }

        if (!(node instanceof PsiDirectoryNode)) {
            return;
        }

        final PsiDirectory directory = ((PsiDirectoryNode) node).getValue();
        final Module module = ModuleUtilCore.findModuleForPsiElement(directory);
        if (module == null) {
            return;
        }

        final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        // Make sure there is at least a root to go off of
        if (rootManager.getContentRoots().length < 1) {
            return;
        }

        // Get the root and compare it to the node
        final VirtualFile root = rootManager.getContentRoots()[0];
        if (!root.equals(node.getVirtualFile())) {
            return;
        }

        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        final String[] path = moduleManager.getModuleGroupPath(module);

        if (path == null) {
            handleNoGroup(project, data, module);
        } else {
            handleGroup(data, module, moduleManager);
        }
    }

    private void handleNoGroup(@NotNull Project project, @NotNull PresentationData data, @NotNull Module module) {
        final MinecraftFacet facet = MinecraftFacet.getInstance(module);
        if (facet == null) {
            // One last attempt for top level nodes
            handleGroup(data, module, ModuleManager.getInstance(project));
            return;
        }

        final Icon icon = facet.getIcon();
        if (icon == null) {
            return;
        }

        data.setIcon(icon);
    }

    private void handleGroup(@NotNull PresentationData data, @NotNull Module module, @NotNull ModuleManager manager) {
        final Module[] modules = manager.getModules();
        for (Module m : modules) {
            if (m.equals(module)) {
                continue;
            }

            final String[] path = manager.getModuleGroupPath(m);
            if (path == null || path.length == 0) {
                continue;
            }

            final String moduleName = path[path.length - 1];
            final Module intermediateModule = manager.findModuleByName(moduleName);

            if (!module.equals(intermediateModule)) {
                continue;
            }

            final MinecraftFacet facet = MinecraftFacet.getInstance(m);
            if (facet == null) {
                continue;
            }

            final Icon icon = facet.getIcon();
            if (icon == null) {
                continue;
            }

            data.setIcon(icon);
            return;
        }
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {}
}
