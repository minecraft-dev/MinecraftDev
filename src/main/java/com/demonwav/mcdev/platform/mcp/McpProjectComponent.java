/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp;

import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.at.AtFileType;
import com.demonwav.mcdev.util.Util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class McpProjectComponent extends AbstractProjectComponent {
    private static final String ACCESS_TRANSFORMER_CLASS =
        "net.minecraftforge.fml.common.asm.transformers.AccessTransformer";

    protected McpProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();

        // Generally the "standard" file extension for access transformers
        // We'll also check for other below
        // We do this here so any other .cfg files don't get marked if they aren't in MCP projects
        Util.runWriteTask(() -> FileTypeManager.getInstance().associateExtension(AtFileType.getInstance(), "cfg"));

        MinecraftModule.doWhenReady(instance -> {
            if (myProject.isDisposed()) {
                return;
            }

            final McpModule mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
            if (mcpModule == null) {
                return;
            }

            ApplicationManager.getApplication().runReadAction(() -> {
                final String[] fileNames = PsiShortNamesCache.getInstance(myProject).getAllFileNames();
                for (final String fileName : fileNames) {
                    if (!fileName.endsWith(".cfg")) {
                        continue;
                    }

                    final PsiFile[] filesByName = PsiShortNamesCache.getInstance(myProject).getFilesByName(fileName);
                    for (final PsiFile psiFile : filesByName) {
                        mcpModule.addAccessTransformerFile(psiFile.getVirtualFile());
                    }
                }
            });
        });


        StartupManager.getInstance(myProject).registerPostStartupActivity(() -> {
            final PsiClass aClass = JavaPsiFacade.getInstance(myProject).findClass(ACCESS_TRANSFORMER_CLASS, GlobalSearchScope.allScope(myProject));
            if (aClass == null) {
                return;
            }

            final Query<PsiClass> search = ClassInheritorsSearch.search(aClass);
            for (PsiClass psiClass : search) {
                final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
                if (module == null) {
                    continue;
                }

                final MinecraftModule instance = MinecraftModule.getInstance(module);
                if (instance == null) {
                    continue;
                }

                final McpModule mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
                if (mcpModule == null) {
                    continue;
                }

                final JavaRecursiveElementWalkingVisitor visitor = new JavaRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                        if (!expression.getMethodExpression().getText().equals("super")) {
                            return;
                        }

                        final PsiExpression[] expressions = expression.getArgumentList().getExpressions();
                        if (expressions.length != 1) {
                            return;
                        }

                        final PsiExpression methodExpression = expressions[0];

                        if (!(methodExpression instanceof PsiLiteralExpressionImpl)) {
                            return;
                        }

                        final PsiLiteralExpressionImpl literalExpression = (PsiLiteralExpressionImpl) methodExpression;

                        final String text = literalExpression.getInnerText();
                        if (text == null) {
                            return;
                        }

                        final Optional<VirtualFile> file = MinecraftModule.searchAllModulesForFile(text, SourceType.RESOURCE);

                        file.ifPresent(f -> Util.runWriteTask(() -> {
                            mcpModule.addAccessTransformerFile(f);
                            FileTypeManager.getInstance().associate(AtFileType.getInstance(), new FileNameMatcher() {
                                @Override
                                public boolean accept(@NonNls @NotNull String fileName) {
                                    return fileName.equals(f.getName());
                                }

                                @NotNull
                                @Override
                                public String getPresentableString() {
                                    return "Access Transformers";
                                }

                            });

                        }));
                    }
                };
                psiClass.accept(visitor);
            }
        });
    }
}
