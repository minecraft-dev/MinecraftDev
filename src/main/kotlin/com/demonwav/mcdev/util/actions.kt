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
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import java.util.Arrays

fun getDataFromActionEvent(e: AnActionEvent): ActionData? {
    fun findModuleForLibrary(file: PsiFile): Module? {
        val virtualFile = file.virtualFile ?: return null

        val index = ProjectFileIndex.SERVICE.getInstance(file.project)

        if (!index.isInLibrarySource(virtualFile) && !index.isInLibraryClasses(virtualFile)) {
            return null
        }

        val orderEntries = index.getOrderEntriesForFile(virtualFile)
        if (orderEntries.isEmpty()) {
            return null
        }

        if (orderEntries.size == 1) {
            return orderEntries[0].ownerModule
        }

        val ownerModules = orderEntries.map { it.ownerModule }.toTypedArray()
        Arrays.sort(ownerModules, ModuleManager.getInstance(file.project).moduleDependencyComparator())
        return ownerModules[0]
    }

    val project = e.project ?: return null
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val caret = e.getData(CommonDataKeys.CARET) ?: return null
    val element = file.findElementAt(caret.offset) ?: return null
    val module = ModuleUtil.findModuleForPsiElement(element) ?: findModuleForLibrary(file) ?: return null
    val instance = MinecraftFacet.getInstance(module) ?: return null

    return ActionData(project, editor, file, element, caret, instance)
}
