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
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

public class MixinLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiClass)) {
            return null;
        }
        PsiClass psiClass = (PsiClass) element;

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule == null) {
            return null;
        }

        if (!minecraftModule.isOfType(MixinModuleType.getInstance())) {
            return null;
        }

        PsiModifierList list = psiClass.getModifierList();
        if (list == null) {
            return null;
        }

        PsiAnnotation annotation = list.findAnnotation("org.spongepowered.asm.mixin.Mixin");
        if (annotation == null) {
            return null;
        }

        PsiIdentifier identifier = psiClass.getNameIdentifier();
        if (identifier == null) {
            return null;
        }

        return new MixinLineMarkerInfo(
            identifier,
            identifier.getTextRange(),
            Pass.UPDATE_ALL,
            getIcon(),
            (mouseEvent, psiElement) -> {
                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
                PsiClass resolve = McPsiUtil.resolveGenericClass(value);
                if (resolve == null) {
                    return;
                }

                Editor editor = FileEditorManager.getInstance(resolve.getProject()).getSelectedTextEditor();
                if (editor == null) {
                    return;
                }

                FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                PsiElement navElement = resolve.getNavigationElement();
                navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(resolve, navElement);
                if (navElement != null) {
                    McEditorUtil.gotoTargetElement(navElement, editor, resolve.getContainingFile());
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
        return "Mixin line marker";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return PlatformAssets.MIXIN;
    }

    private static final class MixinLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
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
        @Contract(pure = true)
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof MixinLineMarkerInfo;
        }

        @NotNull
        @Override
        @Contract(pure = true)
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return PlatformAssets.MIXIN;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return element -> "Multiple Mixins";
        }
    }
}
