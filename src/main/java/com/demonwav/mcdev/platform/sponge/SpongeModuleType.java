package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.sponge.generation.SpongeEventGenerationPanel;
import com.demonwav.mcdev.util.CommonColors;
import com.demonwav.mcdev.util.Util;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class SpongeModuleType extends AbstractModuleType<SpongeModule> {

    private static final String ID = "SPONGE_MODULE_TYPE";
    private static final SpongeModuleType instance = new SpongeModuleType();

    private SpongeModuleType() {
        super("org.spongepowered", "spongeapi");
        CommonColors.applyStandardColors(this.colorMap, "org.spongepowered.api.text.format.TextColors");
        CommonColors.applyStandardColors(this.colorMap, "net.minecraft.util.text.TextFormatting");
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
        if (UIUtil.isUnderDarcula()) {
            return PlatformAssets.SPONGE_ICON;
        } else {
            return PlatformAssets.SPONGE_ICON_DARK;
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener", "org.spongepowered.api.plugin.Plugin");
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener");
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
