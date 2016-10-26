/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight;

import com.demonwav.mcdev.asset.MixinAssets;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.util.McEditorUtil;

import com.google.common.collect.Lists;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.SwingConstants;

public class MixinLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiClass)) {
            return null;
        }
        final PsiClass psiClass = (PsiClass) element;

        if (MixinUtils.getContainingMixinClass(psiClass) == null) {
            return null;
        }

        if (!MixinUtils.isMixinModule(element)) {
            return null;
        }

        final PsiElement identifier = psiClass.getNameIdentifier();
        if (identifier == null) {
            return null;
        }

        return new MixinLineMarkerInfo(
            identifier,
            identifier.getTextRange(),
            Pass.UPDATE_ALL,
            getIcon(),
            (mouseEvent, psiElement) -> {
                final Editor editor = FileEditorManager.getInstance(psiClass.getProject()).getSelectedTextEditor();
                if (editor == null) {
                    return;
                }

                final Map<PsiElement, PsiClass> psiClassMap = MixinUtils.getAllMixedClasses(psiClass);
                if (psiClassMap.isEmpty()) {
                    return;
                }

                // If there's only one, just navigate to that class
                if (psiClassMap.size() == 1) {
                    Map.Entry<PsiElement, PsiClass> entry = psiClassMap.entrySet().iterator().next();
                    PsiElement navElement = entry.getValue().getNavigationElement();
                    navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(entry.getValue(), navElement);
                    if (navElement != null) {
                        McEditorUtil.gotoTargetElement(navElement, editor, psiClass.getContainingFile());
                    }
                    return;
                }

                final class Data {
                    public Icon icon;
                    public String text;
                    public int offset;
                    public Runnable runnable;
                    public Data(Icon icon, String text, int offset, Runnable runnable) {
                        this.icon = icon;
                        this.text = text;
                        this.offset = offset;
                        this.runnable = runnable;
                    }
                }

                // There's more than one, so create a pseudo-popup as if it was a merged icon
                List<Data> infos = Lists.newArrayList();
                for (Map.Entry<PsiElement, PsiClass> entry : psiClassMap.entrySet()) {
                    infos.add(new Data(getIcon(), entry.getKey().getText(), entry.getKey().getTextOffset(), () -> {
                        PsiElement navElement = entry.getValue().getNavigationElement();
                        navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(entry.getValue(), navElement);
                        if (navElement != null) {
                            McEditorUtil.gotoTargetElement(navElement, editor, psiClass.getContainingFile());
                        }
                    }));
                }

                Collections.sort(infos, (o1, o2) -> o1.offset - o2.offset);
                final JBList list = new JBList(infos);
                PopupChooserBuilder builder  = JBPopupFactory.getInstance().createListPopupBuilder(list);
                // Modified Jetbrains code
                list.installCellRenderer(dom -> {
                    if (dom instanceof Data) {
                        Icon icon = ((Data) dom).icon;
                        String text = ((Data)dom).text;

                        final JBLabel label = new JBLabel(text, icon, SwingConstants.LEFT);
                        label.setBorder(IdeBorderFactory.createEmptyBorder(2));
                        return label;
                    }

                    return new JBLabel();
                });
                builder.setItemChoosenCallback(() -> {
                    final Object value = list.getSelectedValue();
                    if (value instanceof Data) {
                        ((Data)value).runnable.run();
                    }
                }).createPopup().show(new RelativePoint(mouseEvent));
                // End Jetbrains code
            }
        );
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    @Nullable
    @Override
    public String getName() {
        return "Mixin line marker";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return MixinAssets.MIXIN_CLASS_ICON;
    }

    public static final class MixinLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        MixinLineMarkerInfo(@NotNull PsiElement element,
                            @NotNull TextRange textRange,
                            int updatePass,
                            @NotNull Icon icon,
                            @Nullable GutterIconNavigationHandler<PsiElement> navHandler) {
            super(
                element,
                textRange,
                icon,
                updatePass,
                (NullableFunction<PsiElement, String>) element1 -> "Go to Mixin class",
                navHandler,
                GutterIconRenderer.Alignment.RIGHT);
        }

        @Override
        public boolean configurePopupAndRenderer(@NotNull PopupChooserBuilder builder, @NotNull JBList list, @NotNull List<MergeableLineMarkerInfo> markers) {
            return super.configurePopupAndRenderer(builder, list, markers);
        }

        @Override
        @Contract(pure = true)
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof MixinLineMarkerInfo;
        }

        @NotNull
        @Override
        @Contract(pure = true)
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return MixinAssets.MIXIN_CLASS_ICON;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return element -> "Go to Mixin class";
        }

        @Override
        public int getCommonUpdatePass(@NotNull List<MergeableLineMarkerInfo> infos) {
            return Pass.UPDATE_ALL;
        }
    }
}
