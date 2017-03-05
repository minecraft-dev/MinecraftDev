/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McActionUtil")
package com.demonwav.mcdev.util

import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtil

fun getDataFromActionEvent(e: AnActionEvent): ActionData? {
    val project = e.project ?: return null
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val caret = e.getData(CommonDataKeys.CARET) ?: return null
    val element = file.findElementAt(caret.offset) ?: return null
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return null
    val instance = MinecraftFacet.getInstance(module) ?: return null

    return ActionData(project, editor, file, element, caret, instance)
}
