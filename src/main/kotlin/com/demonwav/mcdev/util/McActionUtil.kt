/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McActionUtil")
package com.demonwav.mcdev.util

import com.demonwav.mcdev.platform.MinecraftModule
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleManager

fun getDataFromActionEvent(e: AnActionEvent): ActionData? {
    val project = e.project ?: return null

    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null

    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null

    val caret = e.getData(CommonDataKeys.CARET) ?: return null

    val element = file.findElementAt(caret.offset) ?: return null

    val modules = ModuleManager.getInstance(project).modules

    var instance: MinecraftModule? = null
    for (module in modules) {
        instance = MinecraftModule.getInstance(module)
        if (instance != null && instance.isOfType(McpModuleType.getInstance())) {
            break
        }
    }

    if (instance == null) {
        return null
    }

    return ActionData(project, editor, file, element, caret, instance)
}
