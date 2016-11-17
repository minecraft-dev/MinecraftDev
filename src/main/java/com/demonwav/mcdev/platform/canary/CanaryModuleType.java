/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.canary.generation.CanaryHookGenerationPanel;
import com.demonwav.mcdev.platform.canary.util.CanaryConstants;
import com.demonwav.mcdev.platform.canary.util.CanaryLegacyColors;
import com.demonwav.mcdev.util.CommonColors;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class CanaryModuleType extends AbstractModuleType<CanaryModule> {

    private static final CanaryModuleType instance = new CanaryModuleType();

    private static final String ID = "CANARY_MODULE_TYPE";
    private static final List<String> IGNORED_ANNOTATIONS = ImmutableList.of(
            CanaryConstants.HOOK_HANDLER_ANNOTATION,
            CanaryConstants.COMMAND_ANNOTATION,
            CanaryConstants.TAB_COMPLETE_ANNOTATION,
            CanaryConstants.COLUMN_ANNOTATION
    );
    private static final List<String> LISTENER_ANNOTATIONS = ImmutableList.of(CanaryConstants.HOOK_HANDLER_ANNOTATION);

    private CanaryModuleType() {
        this("net.canarymod", "CanaryLib");
    }

    protected CanaryModuleType(final String groupId, final String artifactId) {
        super(groupId, artifactId);
        CommonColors.applyStandardColors(this.colorMap, CanaryConstants.CHAT_FORMAT_CLASS);
        CanaryLegacyColors.applyLegacyColors(this.colorMap, CanaryConstants.LEGACY_COLORS_CLASS);
        CanaryLegacyColors.applyLegacyColors(this.colorMap, CanaryConstants.LEGACY_TEXT_FORMAT_CLASS);
    }

    @NotNull
    public static CanaryModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CANARY;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.CANARY_ICON;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return IGNORED_ANNOTATIONS;
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return LISTENER_ANNOTATIONS;
    }

    @NotNull
    @Override
    public CanaryModule generateModule(Module module) {
        return new CanaryModule<>(module, this);
    }

    @Contract(pure = true)
    @Override
    public boolean isEventGenAvailable() {
        return true;
    }

    @NotNull
    @Override
    public EventGenerationPanel getEventGenerationPanel(@NotNull PsiClass chosenClass) {
        return new CanaryHookGenerationPanel(chosenClass);
    }

}
