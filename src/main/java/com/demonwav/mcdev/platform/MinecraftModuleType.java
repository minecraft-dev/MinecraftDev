package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.creator.MinecraftModuleBuilder;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;

public class MinecraftModuleType extends JavaModuleType {

    @NotNull
    private static final String ID = "MINECRAFT_MODULE_TYPE";
    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;

    public MinecraftModuleType() {
        super(ID);
        this.groupId = "";
        this.artifactId = "";
    }

    public MinecraftModuleType(final String ID, @NotNull final String groupId, @NotNull final String artifactId) {
        super(ID);
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public static MinecraftModuleType getInstance() {
        return (MinecraftModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    public PlatformType getPlatformType() {
        return null;
    }

    @NotNull
    @Override
    public MinecraftModuleBuilder createModuleBuilder() {
        return new MinecraftModuleBuilder();
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.MINECRAFT_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.MINECRAFT_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.MINECRAFT_ICON;
    }

    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    public List<String> getIgnoredAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    public AbstractModule generateModule(Module module) {
        return null;
    }

    public List<String> getListenerAnnotations() {
        return Collections.emptyList();
    }
}
