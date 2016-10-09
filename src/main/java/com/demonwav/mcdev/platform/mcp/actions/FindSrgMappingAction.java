/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mcp.actions;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;
import com.demonwav.mcdev.platform.mcp.srg.SrgMap;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.LightColors;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

public class FindSrgMappingAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            showBalloon(e);
            return;
        }

        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            showBalloon(e);
            return;
        }

        final Caret caret = e.getData(CommonDataKeys.CARET);
        if (caret == null) {
            showBalloon(e);
            return;
        }

        final PsiElement element = file.findElementAt(caret.getOffset());
        if (element == null) {
            showBalloon(e);
            return;
        }

        if (!(element instanceof PsiIdentifier)) {
            showBalloon(e);
            showBalloon(e);
            return;
        }

        final Module[] modules = ModuleManager.getInstance(project).getModules();

        MinecraftModule instance = null;
        for (Module module : modules) {
            instance = MinecraftModule.getInstance(module);
            if (instance != null && instance.isOfType(McpModuleType.getInstance())) {
                break;
            }
        }

        if (instance == null) {
            showBalloon(e);
            return;
        }

        final McpModule mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
        if (mcpModule == null) {
            showBalloon(e);
            return;
        }

        final SrgManager srgManager = SrgManager.getInstance(mcpModule);
        srgManager.recomputeIfNullAndGetSrgMap().done(srgMap -> {
            PsiElement parent = element.getParent();

            final ShadowedMembers shadowedMembers = MixinUtils.getShadowedElement(parent);
            if (!shadowedMembers.getTargets().isEmpty()) {
                parent = shadowedMembers.getTargets().get(0);
            }

            if (parent instanceof PsiField) {
                final PsiField field = (PsiField) parent;

                final String s = SrgMap.toString(field);

                final String fieldMcpToSrg = srgMap.findFieldMcpToSrg(s);
                if (fieldMcpToSrg == null) {
                    showBalloon(e);
                    return;
                }

                final String fieldNameFinal = fieldMcpToSrg.substring(fieldMcpToSrg.lastIndexOf("/") + 1);

                showSuccessBalloon(editor, element, fieldNameFinal);
            } else if (parent instanceof PsiMethod) {
                final PsiMethod method = (PsiMethod) parent;

                final String s = SrgMap.toString(method);

                String methodMcpToSrg = srgMap.findMethodMcpToSrg(s);
                if (methodMcpToSrg == null) {
                    showBalloon(e);
                    return;
                }

                final String preParen = methodMcpToSrg.substring(0, methodMcpToSrg.indexOf('('));
                final String methodName = preParen.substring(preParen.lastIndexOf('/') + 1);
                final String params = methodMcpToSrg.substring(methodMcpToSrg.indexOf('('));

                showSuccessBalloon(editor, element, methodName + params);
            } else if (parent instanceof PsiClass) {
                final PsiClass psiClass = (PsiClass) parent;

                final String s = SrgMap.toString(psiClass);

                final String classMcpToSrg = srgMap.findClassMcpToSrg(s);
                if (classMcpToSrg == null) {
                    showBalloon(e);
                    return;
                }

                showSuccessBalloon(editor, element, classMcpToSrg);
            } else {
                showBalloon(e);
            }
        });
    }

    private void showBalloon(@NotNull AnActionEvent e) {
        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("No mappings found", null, LightColors.YELLOW, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon();

        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));

        balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    private void showSuccessBalloon(@NotNull Editor editor, @NotNull PsiElement element, @NotNull String text) {
        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("SRG name: " + text, null, LightColors.SLIGHTLY_GREEN, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon();

        balloon.show(
            new RelativePoint(
                editor.getContentComponent(),
                editor.visualPositionToXY(editor.offsetToVisualPosition(element.getTextRange().getEndOffset()))
            ),
            Balloon.Position.atRight
        );
    }
}
