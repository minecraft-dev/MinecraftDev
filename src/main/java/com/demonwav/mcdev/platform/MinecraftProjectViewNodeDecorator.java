package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.util.ui.UIUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class sets the icons for the modules in the project view.
 */
public class MinecraftProjectViewNodeDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (!MinecraftSettings.getInstance().isShowProjectPlatformIcons()) {
            return;
        }

        if (node.getProject() == null) {
            return;
        }

        for (Module module : ModuleManager.getInstance(node.getProject()).getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            // Make sure there is at least a root to go off of
            if (rootManager.getContentRoots().length < 1) {
                continue;
            }

            // Get the root and compare it to the node
            VirtualFile root = rootManager.getContentRoots()[0];
            if (!root.equals(node.getVirtualFile())) {
                continue;
            }

            // At this point we know this a module node, now check if it's a valid module for us
            MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
            if (minecraftModule == null) {
                continue;
            }

            // MinecraftModule.getInstance() does us a solid and returns the right module even if we gave it a child
            // module. We don't want that here, so verify that the module we gave it is the module we get back
            if (!minecraftModule.getIdeaModule().equals(module)) {
                continue;
            }

            List<AbstractModuleType<?>> validTypes = minecraftModule.getTypes().stream()
                    .filter(AbstractModuleType::hasIcon)
                    .collect(Collectors.toList());
            if (validTypes.isEmpty()) {
                continue;
            }
            if (validTypes.size() == 1) {
                data.setIcon(validTypes.get(0).getIcon());
                continue;
            }
            if (validTypes.size() == 2) {
                if (validTypes.contains(SpongeModuleType.getInstance()) && validTypes.contains(ForgeModuleType.getInstance())) {
                    if (UIUtil.isUnderDarcula()) {
                        data.setIcon(PlatformAssets.SPONGE_FORGE_ICON);
                    } else {
                        data.setIcon(PlatformAssets.SPONGE_FORGE_ICON_DARK);
                    }
                    continue;
                }
            }
            data.setIcon(PlatformAssets.MINECRAFT_ICON);
        }
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {}
}
