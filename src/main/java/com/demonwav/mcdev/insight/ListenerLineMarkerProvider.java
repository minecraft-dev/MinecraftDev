package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformUtil;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.util.EditSourceUtil;
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
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiExpressionTrimRenderer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * A {@link LineMarkerProviderDescriptor} that will provide a line marker info icon
 * in the gutter for annotated event listeners. This is intended to be written to be
 * platform independent of which Minecraft Platform API is being used.
 *
 * It is unknown whether other JVM languages will function, but there shouldn't be anything
 * broken here.
 *
 * @author gabizou
 */
public class ListenerLineMarkerProvider extends LineMarkerProviderDescriptor {
    private final EditorColorsManager myColorsManager;

    public ListenerLineMarkerProvider(EditorColorsManager colorsManager) {
        myColorsManager = colorsManager;
    }

    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull final PsiElement element) {
        // Since we want to line up with the method declaration, not the annotation
        // declaration, we need to target identifiers, not just PsiMethods.
        if (!(element instanceof PsiIdentifier && (element.getParent() instanceof PsiMethod))) {
            return null;
        }
        // The PsiIdentifier is going to be a method of course!
        PsiMethod method = (PsiMethod) element.getParent();
        if (method.hasModifierProperty(PsiModifier.ABSTRACT) || method.hasModifierProperty(PsiModifier.STATIC)) {
            // I don't think any implementation allows for abstract or static method listeners.
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
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
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
            // Listeners must have at least a single parameter
            return null;
        }
        // Get the type of the parameter so we can start resolving it
        PsiTypeElement psiEventElement = psiParameter.getTypeElement();
        if (psiEventElement == null) {
            return null;
        }
        final PsiType type = psiEventElement.getType();
        // Validate that it is a class reference type, I don't know if this will work with
        // other JVM languages such as Kotlin or Scala, but it might!
        if (!(type instanceof PsiClassReferenceType)) {
            return null;
        }
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        final PsiClass resolve = ((PsiClassReferenceType) type).resolve();
        if (resolve == null) {
            return null;
        }

        // By this point, we can guarantee that the action of "go to declaration" will work
        // since the PsiClass can be resolved, meaning the event listener is listening to
        // a valid event.
        LineMarkerInfo info = new EventLineMarkerInfo(element, element.getTextRange(), this.getIcon(), Pass.UPDATE_ALL, createHanlder(resolve));
        EditorColorsScheme globalScheme = this.myColorsManager.getGlobalScheme();
        info.separatorColor = globalScheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        info.separatorPlacement = SeparatorPlacement.TOP;
        return info;
    }

    // This is a navigation handler that just simply goes and opens up the event's declaration,
    // even if the event target is a nested class.
    private static GutterIconNavigationHandler<PsiElement> createHanlder(PsiClass psiClass) {
        return (e, element1) -> {
            final PsiFile containingFile = psiClass.getContainingFile();
            final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiClass);

            if (virtualFile != null && containingFile != null) {
                final Project project = psiClass.getProject();
                final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                if (editor != null) {
                    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                    PsiElement navElement = psiClass.getNavigationElement();
                    navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(psiClass, navElement);
                    if (navElement != null) {
                        gotoTargetElement(navElement, editor, containingFile);
                    }
                }
            }
        };
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

    private static final class EventLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        EventLineMarkerInfo(@NotNull PsiElement element, @NotNull TextRange range, @NotNull Icon icon, int passId, GutterIconNavigationHandler<PsiElement> handler) {
            super(element, range, icon, passId,
                    (NullableFunction<PsiElement, String>) element1 -> "Go to Event declaration",
                    handler, GutterIconRenderer.Alignment.RIGHT);
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
