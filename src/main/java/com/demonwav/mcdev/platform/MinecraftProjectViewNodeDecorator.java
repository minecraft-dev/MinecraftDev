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
import java.util.Set;

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

        final Set<MinecraftFacet> children = MinecraftFacet.getChildInstances(module);
        if (children.isEmpty()) {
            return;
        }

        final ModuleManager manager = ModuleManager.getInstance(project);
        final String[] path = manager.getModuleGroupPath(module);
        if (path == null) {
            data.setIcon(children.iterator().next().getIcon());
            return;
        }

        final Module testModule = manager.findModuleByName(path[path.length - 1]);
        if (module != testModule) {
            return;
        }

        data.setIcon(children.iterator().next().getIcon());
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {}
}
