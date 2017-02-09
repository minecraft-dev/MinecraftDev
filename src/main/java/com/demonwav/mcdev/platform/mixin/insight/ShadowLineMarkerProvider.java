/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight;

import com.demonwav.mcdev.asset.MixinAssets;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;
import com.demonwav.mcdev.util.McEditorUtil;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

public class ShadowLineMarkerProvider extends LineMarkerProviderDescriptor {
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        final ShadowedMembers shadowedMembers = MixinUtils.getShadowedElement(element);
        if (shadowedMembers.getTargets().isEmpty()) {
            return null;
        }

        if (!(element instanceof PsiNameIdentifierOwner)) {
            return null;
        }

        final PsiIdentifier identifier = (PsiIdentifier) ((PsiNameIdentifierOwner) element).getNameIdentifier();
        if (identifier == null) {
            return null;
        }

        return new ShadowLineMarkerInfo(
            identifier,
            identifier.getTextRange(),
            Pass.UPDATE_ALL,
            getIcon(),
            (mouseEvent, psiElement) -> {
                final Editor editor = FileEditorManager.getInstance(element.getProject()).getSelectedTextEditor();
                if (editor == null) {
                    return;
                }

                // TODO create popup and choose out of the list
                FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                PsiElement navElement = shadowedMembers.getTargets().get(0).getNavigationElement();
                navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(shadowedMembers.getTargets().get(0), navElement);
                if (navElement != null) {
                    McEditorUtil.gotoTargetElement(navElement, editor, element.getContainingFile());
                }
            }
        );
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {

    }

    @Nullable
    @Override
    public String getName() {
        return "Shadow line marker";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return MixinAssets.SHADOW;
    }

    private static final class ShadowLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        ShadowLineMarkerInfo(@NotNull PsiElement element,
                             @NotNull TextRange textRange,
                             int updatePass,
                             @NotNull Icon icon,
                             @Nullable GutterIconNavigationHandler<PsiElement> navHandler) {
            super(
                element,
                textRange,
                icon,
                updatePass,
                (NullableFunction<PsiElement, String>) element1 -> "Go to Shadow element",
                navHandler,
                GutterIconRenderer.Alignment.LEFT
            );
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof ShadowLineMarkerInfo;
        }

        @Override
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return MixinAssets.SHADOW;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return element -> "Multiple Shadows";
        }

        @Override
        public GutterIconRenderer.Alignment getCommonIconAlignment(@NotNull List<MergeableLineMarkerInfo> infos) {
            return GutterIconRenderer.Alignment.LEFT;
        }
    }
}
