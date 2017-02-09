/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.MinecraftSettings;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.codeInsight.daemon.NavigateAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.TwoColorsIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class ColorLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (!MinecraftSettings.Companion.getInstance().isShowChatColorGutterIcons()) {
            return null;
        }

        ColorInfo info =  ColorUtil.findColorFromElement(element, (map, chosen) -> new ColorInfo(element, chosen.getValue(), map));
        if (info != null) {
            NavigateAction.setNavigateAction(info, "Change color", null);
        }

        return info;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    public static class ColorInfo extends MergeableLineMarkerInfo<PsiElement> {

        protected final Color color;

        public ColorInfo(@NotNull final PsiElement element, @NotNull final Color color, @NotNull Map<String, Color> map) {
            super(
                    element,
                    element.getTextRange(),
                    new ColorIcon(12, color),
                    Pass.UPDATE_ALL,
                    FunctionUtil.<Object, String>nullConstant(),
                    (mouseEvent, psiElement) -> {
                        if (!psiElement.isWritable()) {
                            return;
                        }

                        final Editor editor = PsiUtilBase.findEditor(element);
                        if (editor == null) {
                            return;
                        }

                        ColorPicker picker = new ColorPicker(map, editor.getComponent());
                        final String newColor = picker.showDialog();
                        if (newColor != null) {
                            ColorUtil.setColorTo(element, newColor);
                        }
                    },
                    GutterIconRenderer.Alignment.RIGHT
            );
            this.color = color;
        }

        public ColorInfo(@NotNull PsiElement element,
                         @NotNull Color color,
                         @NotNull GutterIconNavigationHandler<PsiElement> handler) {
            super(
                element,
                element.getTextRange(),
                new ColorIcon(12, color),
                Pass.UPDATE_ALL,
                FunctionUtil.<Object, String>nullConstant(),
                handler,
                GutterIconRenderer.Alignment.RIGHT
            );
            this.color = color;
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof ColorInfo;
        }

        @Override
        public GutterIconRenderer.Alignment getCommonIconAlignment(@NotNull List<MergeableLineMarkerInfo> infos) {
            return GutterIconRenderer.Alignment.RIGHT;
        }

        @Override
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            if (infos.size() == 2 && infos.get(0) instanceof ColorInfo && infos.get(1) instanceof ColorInfo) {
                return new TwoColorsIcon(12, ((ColorInfo) infos.get(0)).color, ((ColorInfo) infos.get(1)).color);
            }
            return AllIcons.Gutter.Colors;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return FunctionUtil.nullConstant();
        }
    }
}
