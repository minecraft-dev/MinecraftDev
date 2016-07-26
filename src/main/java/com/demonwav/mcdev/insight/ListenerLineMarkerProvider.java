package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.asset.PlatformAssets;
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiExpressionTrimRenderer;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

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

    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull final PsiElement element) {
        if (!MinecraftSettings.getInstance().isShowEventListenerGutterIcons()) {
            return null;
        }

        final Pair<PsiClass, PsiMethod> listener = InsightUtil.getEventListenerFromElement(element);
        if (listener == null) {
            return null;
        }
        // By this point, we can guarantee that the action of "go to declaration" will work
        // since the PsiClass can be resolved, meaning the event listener is listening to
        // a valid event.
        return new EventLineMarkerInfo(
            element,
            element.getTextRange(),
            getIcon(),
            Pass.UPDATE_ALL,
            createHandler(listener.getSecond())
        );
    }

    // This is a navigation handler that just simply goes and opens up the event's declaration,
    // even if the event target is a nested class.
    @NotNull
    private static GutterIconNavigationHandler<PsiElement> createHandler(PsiMethod method) {
        return (e, element1) -> {
            // We need to re-evaluate the targeted method, because if the method signature slightly changes before
            // IntelliJ decides to re-evaluate the method, but the class is no longer valid.
            // In this circumstance, we can find the class anyways because it's still a valid listener.
            final PsiFile containingFile = element1.getContainingFile();
            final Pair<PsiParameter, PsiClass> parameter = InsightUtil.getEventParameterPairFromMethod(method);
            if (parameter == null) {
                return;
            }
            final PsiClass resolve = parameter.second;
            final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(resolve);

            if (virtualFile != null && containingFile != null) {
                final Project project = method.getProject();
                final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                if (editor != null) {
                    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");
                    PsiElement navElement = resolve.getNavigationElement();
                    navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(resolve, navElement);
                    if (navElement != null) {
                        McEditorUtil.gotoTargetElement(navElement, editor, containingFile);
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
        EventLineMarkerInfo(@NotNull PsiElement element,
                            @NotNull TextRange range,
                            @NotNull Icon icon,
                            int passId,
                            GutterIconNavigationHandler<PsiElement> handler) {
            super(
                element,
                range,
                icon,
                passId,
                (NullableFunction<PsiElement, String>) element1 -> "Go to Event declaration",
                handler,
                GutterIconRenderer.Alignment.RIGHT
            );
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            if (!(info instanceof EventLineMarkerInfo)) {
                return false;
            }

            PsiElement otherElement = info.getElement();
            PsiElement myElement = getElement();
            return otherElement != null && myElement != null;
        }

        @Override
        @Contract(pure = true)
        public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
            return myIcon;
        }

        @NotNull
        @Override
        @Contract(pure = true)
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
}
