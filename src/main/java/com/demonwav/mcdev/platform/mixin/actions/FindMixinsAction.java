package com.demonwav.mcdev.platform.mixin.actions;

import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.util.McEditorUtil;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.AllClassesSearchExecutor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FindMixinsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        final Project project = e.getProject();

        final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            return;
        }

        final Caret caret = e.getData(CommonDataKeys.CARET);
        if (caret == null) {
            return;
        }

        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        final PsiElement element = file.findElementAt(caret.getOffset());
        final PsiClass classOfElement = McPsiUtil.getClassOfElement(element);

        if (classOfElement == null) {
            return;
        }

        Set<PsiClass> classes = new HashSet<>();

        // Get on the main thread
        ApplicationManager.getApplication().invokeLater(() -> {
            // Get off the main thread
            ProgressManager.getInstance().run(
                new Task.Backgroundable(project, "Searching for Mixins", true, null) {
                    @Override
                    public boolean shouldStartInBackground() {
                        return false;
                    }

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        // Get permission
                        final AccessToken token = ApplicationManager.getApplication().acquireReadActionLock();
                        indicator.setIndeterminate(true);

                        final AllClassesSearchExecutor executor = new AllClassesSearchExecutor();
                        executor.execute(
                            new AllClassesSearch.SearchParameters(GlobalSearchScope.projectScope(project), project),
                            psiClass -> {
                                indicator.setText("Checking " + psiClass.getName() + "...");

                                final Map<PsiElement, PsiClass> allMixedClasses = MixinUtils.getAllMixedClasses(psiClass);

                                if (allMixedClasses.values().stream()
                                    .anyMatch(c -> c.getQualifiedName() != null &&
                                        c.getQualifiedName().equals(classOfElement.getQualifiedName())
                                    )) {
                                    classes.add(psiClass);
                                }
                                return true;
                            });
                        token.finish();

                        ApplicationManager.getApplication().invokeLater(() -> {
                            McEditorUtil.gotoTargetElement(classes.iterator().next(), editor, file);
                        });
                    }
                }
            );
        });
    }
}
