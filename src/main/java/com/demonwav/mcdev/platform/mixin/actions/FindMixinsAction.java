/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions;

import com.demonwav.mcdev.asset.MixinAssets;
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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.AllClassesSearchExecutor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FindMixinsAction extends AnAction {
    private static final String TOOL_WINDOW_ID = "Find Mixins";

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
                        final Set<PsiClass> classes = new HashSet<>();
                        // Get permission
                        try (final AccessToken ignored = ApplicationManager.getApplication().acquireReadActionLock()) {
                            indicator.setIndeterminate(true);

                            final AllClassesSearchExecutor executor = new AllClassesSearchExecutor();
                            executor.execute(
                                new AllClassesSearch.SearchParameters(GlobalSearchScope.projectScope(project), project),
                                psiClass -> {
                                    indicator.setText("Checking " + psiClass.getName() + "...");

                                    final Map<PsiElement, PsiClass> allMixedClasses = MixinUtils.getAllMixedClasses(psiClass);

                                    if (allMixedClasses.values().stream()
                                        .anyMatch(c -> c.getQualifiedName() != null &&
                                            c.getQualifiedName().equals(classOfElement.getQualifiedName()))
                                    ) {
                                        classes.add(psiClass);
                                    }
                                    return true;
                                }
                            );
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (classes.size() == 1) {
                                McEditorUtil.gotoTargetElement(classes.iterator().next(), editor, file);
                            } else {
                                ToolWindowManager.getInstance(project).unregisterToolWindow(TOOL_WINDOW_ID);
                                final ToolWindow window = ToolWindowManager.getInstance(project).registerToolWindow(TOOL_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
                                window.setIcon(MixinAssets.MIXIN_CLASS_ICON);

                                // Sort the results so it appears nicer
                                final List<PsiClass> classesList = new ArrayList<>(classes);
                                Collections.sort(classesList, (c1, c2) -> {
                                    final Pair<String, PsiClass> pair1 = McPsiUtil.getNameOfClass(c1);
                                    final Pair<String, PsiClass> pair2 = McPsiUtil.getNameOfClass(c2);

                                    if (pair1 == null && pair2 == null) {
                                        return 0;
                                    } else if (pair1 == null) {
                                        return -1;
                                    } else if (pair2 == null) {
                                        return 1;
                                    }

                                    final String name1 = pair1.getSecond().getName() + pair1.getFirst();
                                    final String name2 = pair2.getSecond().getName() + pair2.getFirst();

                                    return name1.compareTo(name2);
                                });

                                final FindMixinsComponent component = new FindMixinsComponent(classesList);

                                final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                                final Content content = contentFactory.createContent(component.getPanel(), null, false);
                                window.getContentManager().addContent(content);

                                window.activate(null);
                            }
                        });
                    }
                }
            );
        });
    }
}
