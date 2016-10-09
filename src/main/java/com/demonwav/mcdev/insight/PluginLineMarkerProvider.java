/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.asset.GeneralAssets;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PluginLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Nullable
    @Override
    public String getName() {
        return "Minecraft Plugin line marker";
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        final MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return null;
        }

        if (!instance.shouldShowPluginIcon(element)) {
            return null;
        }

        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            GeneralAssets.PLUGIN,
            Pass.UPDATE_ALL,
            FunctionUtil.nullConstant(),
            null,
            GutterIconRenderer.Alignment.RIGHT
        );
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {}
}
