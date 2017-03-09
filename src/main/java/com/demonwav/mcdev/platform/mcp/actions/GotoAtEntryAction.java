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
import com.demonwav.mcdev.platform.mixin.util.Shadow;
import com.demonwav.mcdev.util.ActionData;
import com.demonwav.mcdev.util.McActionUtil;
import com.demonwav.mcdev.util.McEditorUtil;
import com.demonwav.mcdev.util.MemberReference;
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
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.ui.LightColors;
import com.intellij.ui.awt.RelativePoint;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

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

        final McpModule mcpModule = data.getInstance().getModuleOfType(McpModuleType.INSTANCE);
        if (mcpModule == null) {
            showBalloon(e);
            return;
        }

        mcpModule.getSrgManager().getSrgMap().done(srgMap -> {
            PsiElement parent = data.getElement().getParent();

            if (parent instanceof PsiMember) {
                PsiMember shadowTarget = Shadow.findFirstShadowTarget((PsiMember) parent);
                if (shadowTarget != null) {
                    parent = shadowTarget;
                }
            }

            if (parent instanceof PsiField) {
                final MemberReference reference = srgMap.findSrgField((PsiField) parent);
                if (reference == null) {
                    showBalloon(e);
                    return;
                }

                searchForText(mcpModule, e, data, reference.getName());
            } else if (parent instanceof PsiMethod) {
                MemberReference reference = srgMap.findSrgMethod((PsiMethod) parent);
                if (reference == null) {
                    showBalloon(e);
                    return;
                }

                searchForText(mcpModule, e, data, reference.getName() + reference.getDescriptor());
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

        ApplicationManager.getApplication()
                          .invokeLater(() -> balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight));
    }
}
