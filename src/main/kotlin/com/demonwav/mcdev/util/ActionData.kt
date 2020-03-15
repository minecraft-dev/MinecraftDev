/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ActionData(
    val project: Project,
    val editor: Editor,
    val file: PsiFile,
    val element: PsiElement,
    val caret: Caret,
    val instance: MinecraftFacet
)
