/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions;

import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;
import com.demonwav.mcdev.util.ActionData;
import com.demonwav.mcdev.util.McActionUtil;
import com.demonwav.mcdev.util.MemberReference;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.LightColors;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

public class FindSrgMappingAction extends AnAction {
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

        mcpModule.getSrgManager().getSrgMap().done(srgMap -> {
            PsiElement parent = data.getElement().getParent();

            final ShadowedMembers shadowedMembers = MixinUtils.getShadowedElement(parent);
            if (!shadowedMembers.getTargets().isEmpty()) {
                parent = shadowedMembers.getTargets().get(0);
            }

            if (parent instanceof PsiField) {
                final MemberReference srg = srgMap.findSrgField((PsiField) parent);
                if (srg == null) {
                    showBalloon(e);
                    return;
                }

                showSuccessBalloon(data.getEditor(), data.getElement(), srg.getName());
            } else if (parent instanceof PsiMethod) {
                MemberReference srg = srgMap.findSrgMethod((PsiMethod) parent);
                if (srg == null) {
                    showBalloon(e);
                    return;
                }

                showSuccessBalloon(data.getEditor(), data.getElement(), srg.getName() + srg.getDescriptor());
            } else if (parent instanceof PsiClass) {
                final String classMcpToSrg = srgMap.findSrgClass((PsiClass) parent);
                if (classMcpToSrg == null) {
                    showBalloon(e);
                    return;
                }

                showSuccessBalloon(data.getEditor(), data.getElement(), classMcpToSrg);
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

        ApplicationManager.getApplication().invokeLater(() ->
            balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight));
    }

    private void showSuccessBalloon(@NotNull Editor editor, @NotNull PsiElement element, @NotNull String text) {
        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("SRG name: " + text, null, LightColors.SLIGHTLY_GREEN, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon();

        ApplicationManager.getApplication().invokeLater(() -> balloon.show(
            new RelativePoint(
                editor.getContentComponent(),
                editor.visualPositionToXY(editor.offsetToVisualPosition(element.getTextRange().getEndOffset()))
            ),
            Balloon.Position.atRight
        ));
    }
}
