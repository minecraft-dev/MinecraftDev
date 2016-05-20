package com.demonwav.mcdev.platform;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PlatformUtil {
    private static final Map<VirtualFile, AbstractModule> map = new HashMap<>();

    @Nullable
    public static AbstractModule getInstance(@NotNull Module module) {
        VirtualFile moduleRoot = ModuleRootManager.getInstance(module).getContentRoots()[0];

        ModuleType moduleType = ModuleUtil.getModuleType(module);

        if (moduleType instanceof MinecraftModuleType) {
            return map.computeIfAbsent(moduleRoot, m -> ((MinecraftModuleType) moduleType).generateModule(module));
        } else { // last ditch effort for gradle multi projects
            String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
            if (paths != null && paths.length >= 1) {
                // The last element will be this module, the second to last is the parent
                String parentName = paths[paths.length - 1];
                Module parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(parentName);
                if (parentModule != null) {
                    return getInstance(parentModule);
                }
            }
        }
        return null;
    }
}
