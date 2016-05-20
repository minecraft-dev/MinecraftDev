package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformUtil;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.daemon.*;
import com.intellij.codeInsight.daemon.impl.JavaLineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiExpressionTrimRenderer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

public class ListenerLineMarkerProvider extends LineMarkerProviderDescriptor {
    private final EditorColorsManager myColorsManager;

    public ListenerLineMarkerProvider(EditorColorsManager colorsManager) {
        myColorsManager = colorsManager;
    }

    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull final PsiElement element) {
        if (!(element instanceof PsiIdentifier && (element.getParent() instanceof PsiMethod))) {
            return null;
        }
        PsiMethod method = (PsiMethod) element.getParent();
        if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return null;
        }
        PsiModifierList modifierList = method.getModifierList();
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }
        AbstractModule instance = PlatformUtil.getInstance(module);
        if (instance == null) {
            return null;
        }
        final List<String> listenerAnnotations = instance.getModuleType().getListenerAnnotations();
        boolean contains = false;
        for (String listenerAnnotation : listenerAnnotations) {
            if (modifierList.findAnnotation(listenerAnnotation) != null) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            return null;
        }
        final PsiParameter psiParameter = method.getParameterList().getParameters()[0];
        if (psiParameter == null) {
            return null;
        }
        PsiElement psiEventElement = psiParameter.getTypeElement();
        if (psiEventElement == null) {
            return null;
        }

        LineMarkerInfo<PsiElement> info = new EventLineMarkerInfo(psiEventElement, element.getTextRange(), this.getIcon(), Pass.UPDATE_ALL);
        EditorColorsScheme globalScheme = this.myColorsManager.getGlobalScheme();
        info.separatorColor = globalScheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        info.separatorPlacement = SeparatorPlacement.TOP;
        return info;

    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    @NotNull
    @Override
    public String getName() {
        return "Event Listener line marker";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return PlatformAssets.LISTENER;
    }

    public static final class EventLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        private EventLineMarkerInfo(@NotNull PsiElement element, @NotNull TextRange range, @NotNull Icon icon, int passId) {
            super(element, range, icon, passId,
                    (NullableFunction<PsiElement, String>) element1 -> "Go to event class",
                    (e, element1) -> {
                        final PsiFile containingFile = element.getContainingFile();
                        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);

                        if (virtualFile != null && containingFile != null) {
                            final Project project = element.getProject();
                            final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                            if (editor != null) {
                                FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                                PsiElement navElement = element.getNavigationElement();
                                navElement = navElement.getFirstChild();
                                navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(element, navElement);
                                if (navElement != null) {
                                    gotoTargetElement(navElement, editor, containingFile);
                                }
                            }
                        }
                    },
                    GutterIconRenderer.Alignment.RIGHT);
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            if (!(info instanceof EventLineMarkerInfo)) return false;
            PsiElement otherElement = info.getElement();
            PsiElement myElement = getElement();
            return otherElement != null && myElement != null;
        }


        @Override
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return myIcon;
        }

        @NotNull
        @Override
        public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
            return (Function<PsiElement, String>) element -> "Multiple method overrides";
        }

        @Override
        public String getElementPresentation(PsiElement element) {
            final PsiElement parent = element.getParent();
            if (parent instanceof PsiFunctionalExpression) {
                return PsiExpressionTrimRenderer.render((PsiExpression)parent);
            }
            return super.getElementPresentation(element);
        }
    }

    static void gotoTargetElement(@NotNull PsiElement element, @NotNull Editor currentEditor, @NotNull PsiFile currentFile) {
        if (element.getContainingFile() == currentFile) {
            int offset = element.getTextOffset();
            PsiElement leaf = currentFile.findElementAt(offset);
            // check that element is really physically inside the file
            // there are fake elements with custom navigation (e.g. opening URL in browser) that override getContainingFile for various reasons
            if (leaf != null && PsiTreeUtil.isAncestor(element, leaf, false)) {
                Project project = element.getProject();
                IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                new OpenFileDescriptor(project, currentFile.getViewProvider().getVirtualFile(), offset).navigateIn(currentEditor);
                return;
            }
        }

        Navigatable navigatable = element instanceof Navigatable ? (Navigatable)element : EditSourceUtil.getDescriptor(element);
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }


}
