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

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public final class ActionData {

    private final Project project;
    private final Editor editor;
    private final PsiFile file;
    private final PsiElement element;
    private final Caret caret;
    private final MinecraftModule instance;

    public ActionData(Project project, Editor editor, PsiFile file, PsiElement element, Caret caret, MinecraftModule instance) {
        this.project = project;
        this.editor = editor;
        this.file = file;
        this.element = element;
        this.caret = caret;
        this.instance = instance;
    }

    public Project getProject() {
        return project;
    }

    public Editor getEditor() {
        return editor;
    }

    public PsiFile getFile() {
        return file;
    }

    public PsiElement getElement() {
        return element;
    }

    public Caret getCaret() {
        return caret;
    }

    public MinecraftModule getInstance() {
        return instance;
    }
}
