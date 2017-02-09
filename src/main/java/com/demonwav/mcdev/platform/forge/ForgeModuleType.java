/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.demonwav.mcdev.util.Util;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class ForgeModuleType extends AbstractModuleType<ForgeModule> {

    private static final ForgeModuleType instance = new ForgeModuleType();

    private static final String ID = "FORGE_MODULE_TYPE";
    private static final List<String> IGNORED_ANNOTATIONS = ImmutableList.of(
            ForgeConstants.MOD_ANNOTATION,
            ForgeConstants.EVENT_HANDLER_ANNOTATION,
            ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    );
    private static final List<String> LISTENER_ANNOTATIONS = ImmutableList.of(
            ForgeConstants.EVENT_HANDLER_ANNOTATION,
            ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION
    );

    private ForgeModuleType() {
        super("", "");
    }

    @NotNull
    public static ForgeModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
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
    public ForgeModule generateModule(@NotNull Module module) {
        return new ForgeModule(module);
    }

    @NotNull
    @Override
    public String getDefaultListenerName(@NotNull PsiClass psiClass) {
        return Util.defaultNameForSubClassEvents(psiClass);
    }

    @Override
    public boolean isEventGenAvailable() {
        return true;
    }
}
