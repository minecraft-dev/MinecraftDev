package com.demonwav.mcdev.platform.mixin.insight;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierList;
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
        if (!(element instanceof PsiField)) {
            return null;
        }
        final PsiField field = (PsiField) element;

        final Module module = ModuleUtilCore.findModuleForPsiElement(field);
        if (module == null) {
            return null;
        }

        final MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return null;
        }

        if (!instance.isOfType(MixinModuleType.getInstance())) {
            return null;
        }

        final PsiClass containingClass = McPsiUtil.getClassOfElement(field);
        if (containingClass == null) {
            return null;
        }

        final PsiModifierList classList = containingClass.getModifierList();
        if (classList == null) {
            return null;
        }

        final PsiAnnotation classAnnotation = classList.findAnnotation("org.spongepowered.asm.mixin.Mixin");
        if (classAnnotation == null) {
            return null;
        }

        final PsiModifierList list = field.getModifierList();
        if (list == null) {
            return null;
        }

        final PsiAnnotation annotation = list.findAnnotation("org.spongepowered.asm.mixin.Shadow");
        if (annotation == null) {
            return null;
        }

        final PsiIdentifier identifier = field.getNameIdentifier();

        final PsiAnnotationMemberValue value = classAnnotation.findDeclaredAttributeValue("value");
        if (value == null) {
            return null;
        }

        return new ShadowLineMarkerInfo(
            identifier,
            identifier.getTextRange(),
            Pass.UPDATE_ALL,
            getIcon(),
            (mouseEvent, psiElement) -> {
                final PsiClass resolve = McPsiUtil.resolveGenericClass(value);
                if (resolve == null) {
                    return;
                }

                final PsiField resolveField = resolve.findFieldByName(identifier.getText(), false);
                if (resolveField == null) {
                    return;
                }

                final Editor editor = FileEditorManager.getInstance(resolve.getProject()).getSelectedTextEditor();
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
