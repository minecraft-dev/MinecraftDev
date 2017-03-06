/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp;

import com.demonwav.mcdev.facet.MinecraftFacet;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;
import com.google.common.collect.Sets;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import java.util.Set;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public class McpModule extends AbstractModule {

    private final McpModuleSettings settings;
    private final Set<VirtualFile> accessTransformers = Sets.newHashSet();
    private SrgManager srgManager = new SrgManager();

    public McpModule(@NotNull MinecraftFacet facet) {
        super(facet);

        this.settings = McpModuleSettings.getInstance(module);
        srgManager.parse(getSettings().getMappingFiles());
    }

    @Override
    public McpModuleType getModuleType() {
        return McpModuleType.INSTANCE;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.MCP;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "";
    }

    public McpModuleSettings.State getSettings() {
        return settings.getState();
    }

    public SrgManager getSrgManager() {
        return srgManager;
    }

    public void updateSettings(McpModuleSettings.State data) {
        this.settings.loadState(data);
        srgManager.parse(data.getMappingFiles());
    }

    public Set<VirtualFile> getAccessTransformers() {
        return accessTransformers;
    }

    public void addAccessTransformerFile(@NotNull VirtualFile file) {
        accessTransformers.add(file);
    }
}
