/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions;

import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;
import com.demonwav.mcdev.platform.mcp.srg.SrgMap;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;
import com.demonwav.mcdev.util.ActionData;
import com.demonwav.mcdev.util.McActionUtil;
import com.demonwav.mcdev.util.McEditorUtil;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.ui.LightColors;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class GotoAtEntryAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final ActionData data = McActionUtil.getDataFromActionEvent(e);
        if (data == null) {
            showBalloon(e);
            return;
        }

        if (!(data.getElement() instanceof PsiIdentifier)) {
            showBalloon(e);
            return;
        }

        final McpModule mcpModule = data.getInstance().getModuleOfType(McpModuleType.getInstance());
        if (mcpModule == null) {
            showBalloon(e);
            return;
        }

        final SrgManager srgManager = SrgManager.getInstance(mcpModule);
        srgManager.recomputeIfNullAndGetSrgMap().done(srgMap -> {
            PsiElement parent = data.getElement().getParent();

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

                searchForText(mcpModule, e, data, fieldMcpToSrg.substring(fieldMcpToSrg.lastIndexOf('/') + 1));
            } else if (parent instanceof PsiMethod) {
                final PsiMethod method = (PsiMethod) parent;

                final String s = SrgMap.toString(method);

                final String methodMcpToSrg = srgMap.findMethodMcpToSrg(s);
                if (methodMcpToSrg == null) {
                    showBalloon(e);
                    return;
                }

                final String beforeParen = methodMcpToSrg.substring(0, methodMcpToSrg.indexOf('('));
                searchForText(mcpModule, e, data, beforeParen.substring(beforeParen.lastIndexOf('/') + 1));
            } else {
                showBalloon(e);
            }
        });
    }

    private void searchForText(@NotNull McpModule mcpModule, @NotNull AnActionEvent e, @NotNull ActionData data, @NotNull String text) {
        for (VirtualFile virtualFile : mcpModule.getAccessTransformers()) {
            final PsiFile file = PsiManager.getInstance(data.getProject()).findFile(virtualFile);
            if (file == null) {
                continue;
            }

            final AtomicBoolean found = new AtomicBoolean(false);
            PsiSearchHelper.SERVICE.getInstance(data.getProject())
                .processElementsWithWord(
                    (element, offsetInElement) -> {
                        McEditorUtil.gotoTargetElement(element, data.getEditor(), data.getFile());
                        found.set(true);
                        return false;
                    },
                    new LocalSearchScope(file),
                    text,
                    UsageSearchContext.ANY,
                    true
                );

            if (found.get()) {
                return;
            }
        }
        showBalloon(e);
    }

    private void showBalloon(@NotNull AnActionEvent e) {
        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("No access transformer entry found", null, LightColors.YELLOW, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon();

        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));

        ApplicationManager.getApplication().invokeLater(() -> {
            balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
        });
    }
}
