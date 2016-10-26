/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public final class McActionUtil {

    private McActionUtil() {
    }

    public static ActionData getDataFromActionEvent(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return null;
        }

        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }

        final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            return null;
        }

        final Caret caret = e.getData(CommonDataKeys.CARET);
        if (caret == null) {
            return null;
        }

        final PsiElement element = file.findElementAt(caret.getOffset());
        if (element == null) {
            return null;
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
            return null;
        }

        return new ActionData(project, editor, file, element, caret, instance);
    }
}
