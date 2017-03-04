/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class SideOnlyProjectComponent extends AbstractProjectComponent {
    protected SideOnlyProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        MinecraftModule.doWhenReady(instance -> {
            if (myProject.isDisposed()) {
                return;
            }

            if (instance.getIdeaModule().isDisposed()) {
                return;
            }

            if (!instance.isOfType(ForgeModuleType.INSTANCE)) {
                return;
            }

            DumbService.getInstance(myProject).smartInvokeLater(() -> {
                ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Indexing @SidedProxy", true, null) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try (final AccessToken ignored = ApplicationManager.getApplication().acquireReadActionLock()) {
                            indicator.setIndeterminate(true);
                            final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
                            final PsiClass sidedProxy = JavaPsiFacade.getInstance(myProject).findClass(ForgeConstants.SIDED_PROXY_ANNOTATION, scope);
                            if (sidedProxy == null) {
                                return;
                            }
                            final Collection<PsiField> annotatedFields = AnnotatedElementsSearch.searchPsiFields(sidedProxy, scope).findAll();
                            indicator.setIndeterminate(false);
                            double index = 0;
                            for (PsiField field : annotatedFields) {
                                SidedProxyAnnotator.check(field);
                                index++;
                                indicator.setFraction(index / annotatedFields.size());
                            }
                        }
                    }
                });
            });
        });
    }
}
