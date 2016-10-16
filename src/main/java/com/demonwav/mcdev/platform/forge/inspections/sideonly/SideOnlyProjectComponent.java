/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.impl.search.AllClassesSearchExecutor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import org.jetbrains.annotations.NotNull;

public class SideOnlyProjectComponent extends AbstractProjectComponent {
    protected SideOnlyProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        MinecraftModule.doWhenReady(instance -> {
            if (instance.getIdeaModule().isDisposed()) {
                return;
            }

            if (!instance.isOfType(ForgeModuleType.getInstance())) {
                return;
            }

            DumbService.getInstance(myProject).smartInvokeLater(() -> {
                ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Indexing @SidedProxy", true, null) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try (final AccessToken ignored = ApplicationManager.getApplication().acquireReadActionLock()) {
                            indicator.setIndeterminate(true);
                            final JavaRecursiveElementWalkingVisitor visitor = new JavaRecursiveElementWalkingVisitor() {
                                @Override
                                public void visitField(PsiField field) {
                                    super.visitField(field);
                                    SidedProxyAnnotator.check(field);
                                }
                            };

                            final AllClassesSearchExecutor executor = new AllClassesSearchExecutor();
                            executor.execute(
                                new AllClassesSearch.SearchParameters(GlobalSearchScope.projectScope(myProject), myProject),
                                psiClass -> {
                                    psiClass.acceptChildren(visitor);
                                    return true;
                                }
                            );
                        }
                    }
                });
            });
        });
    }
}
