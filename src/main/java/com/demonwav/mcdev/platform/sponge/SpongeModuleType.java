/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.sponge.generation.SpongeEventGenerationPanel;
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants;
import com.demonwav.mcdev.util.CommonColors;
import com.demonwav.mcdev.util.Util;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class SpongeModuleType extends AbstractModuleType<SpongeModule> {

    private static final String ID = "SPONGE_MODULE_TYPE";
    private static final SpongeModuleType instance = new SpongeModuleType();

    private SpongeModuleType() {
        super("org.spongepowered", "spongeapi");
        CommonColors.applyStandardColors(this.colorMap, SpongeConstants.TEXT_COLORS);
        CommonColors.applyStandardColors(this.colorMap, SpongeConstants.TEXT_FORMATTING);
    }


    public static SpongeModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.SPONGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.SPONGE_ICON;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of(SpongeConstants.LISTENER_ANNOTATION, SpongeConstants.PLUGIN_ANNOTATION);
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of(SpongeConstants.LISTENER_ANNOTATION);
    }

    @NotNull
    @Override
    public String getDefaultListenerName(@NotNull PsiClass psiClass) {
        return Util.defaultNameForSubClassEvents(psiClass);
    }

    @NotNull
    @Override
    public SpongeModule generateModule(Module module) {
        return new SpongeModule(module);
    }

    @Override
    public boolean isEventGenAvailable() {
        return true;
    }

    @NotNull
    @Override
    public EventGenerationPanel getEventGenerationPanel(@NotNull PsiClass chosenClass) {
        return new SpongeEventGenerationPanel(chosenClass);
    }
}
