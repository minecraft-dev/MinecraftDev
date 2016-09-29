package com.demonwav.mcdev.platform.mixin.insight;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.mixin.util.MixinConstants;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.util.McEditorUtil;
import com.demonwav.mcdev.util.McPsiUtil;

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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class ShadowLineMarkerProvider extends LineMarkerProviderDescriptor {
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiField)) {
            return null;
        }
        final PsiField field = (PsiField) element;

        if (!MixinUtils.isMixinModule(element)) {
            return null;
        }

        final PsiClass containingClass = McPsiUtil.getClassOfElement(field);

        final PsiAnnotationMemberValue value = MixinUtils.getMixinAnnotationValue(containingClass);
        if (value == null) {
            return null;
        }

        final PsiAnnotation annotation = McPsiUtil.getAnnotation(field, MixinConstants.Annotations.SHADOW);
        if (annotation == null) {
            return null;
        }

        final PsiIdentifier identifier = field.getNameIdentifier();

        return new ShadowLineMarkerInfo(
            identifier,
            identifier.getTextRange(),
            Pass.UPDATE_ALL,
            getIcon(),
            (mouseEvent, psiElement) -> {
                final Map<PsiElement, PsiClass> psiClassMap = MixinUtils.resolveGenericClass(value);
                for (Map.Entry<PsiElement, PsiClass> entry : psiClassMap.entrySet()) {
                    final PsiField resolveField = entry.getValue().findFieldByName(identifier.getText(), false);
                    if (resolveField == null) {
                        continue;
                    }

                    final Editor editor = FileEditorManager.getInstance(entry.getValue().getProject()).getSelectedTextEditor();
                    if (editor == null) {
                        return;
                    }

                    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                    PsiElement navElement = resolveField.getNavigationElement();
                    navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(resolveField, navElement);
                    if (navElement != null) {
                        McEditorUtil.gotoTargetElement(navElement, editor, resolveField.getContainingFile());
                    }
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
        return PlatformAssets.SHADOW;
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
                (NullableFunction<PsiElement, String>) element1 -> "Go to Shadow field",
                navHandler,
                GutterIconRenderer.Alignment.RIGHT
            );
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof ShadowLineMarkerInfo;
        }

        @Override
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return PlatformAssets.SHADOW;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return element -> "Multiple Shadows";
        }
    }
}
