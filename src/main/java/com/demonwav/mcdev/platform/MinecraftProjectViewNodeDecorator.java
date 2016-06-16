package com.demonwav.mcdev.platform;

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

import java.util.Iterator;

/**
 * This class sets the icons for the modules in the project view.
 */
public class MinecraftProjectViewNodeDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
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

            // We use an iterator to get the element(s) from the collection, to avoid any list creation
            Iterator<AbstractModuleType<?>> typeIterator = minecraftModule.getTypes().iterator();
            // This shouldn't happen, but as a safety check
            if (!typeIterator.hasNext()) {
                continue;
            }

            // We will use this type if it turns out this collection only has one item in it
            AbstractModuleType<?> type = typeIterator.next();

            if (typeIterator.hasNext()) {
                // Sponge Forge has it's own special icon
                if (type.equals(SpongeModuleType.getInstance()) || type.equals(ForgeModuleType.getInstance())) {
                    AbstractModuleType<?> next = typeIterator.next();

                    // The first needs to be either sponge or forge, and the second needs to be either sponge or forge
                    // We don't worry about duplicates here for simplicity's sake
                    // We only want to apply the special icon if it's only sponge and forge, so these need to be the only two types
                    if ((next.equals(SpongeModuleType.getInstance()) || next.equals(SpongeModuleType.getInstance())) && !typeIterator.hasNext()) {
                        data.setIcon(PlatformAssets.SPONGE_FORGE_ICON);
                        break;
                    }
                }
                // There are more than one type in this collection, so use a minecraft icon
                data.setIcon(PlatformAssets.MINECRAFT_ICON);
            } else {
                // There is only one type in this collection, so use it's icon
                data.setIcon(type.getIcon());
            }
            // After we've set the icon, no need to continue
            break;
        }
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {}
}
