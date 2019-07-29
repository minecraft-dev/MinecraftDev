/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findReferencedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.gotoTargetElement
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.CARET
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.ui.content.ContentFactory

class FindMixinsAction : AnAction() {

    companion object {
        private const val TOOL_WINDOW_ID = "Find Mixins"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PROJECT) ?: return
        val file = e.getData(PSI_FILE) ?: return
        val caret = e.getData(CARET) ?: return
        val editor = e.getData(EDITOR) ?: return

        val element = file.findElementAt(caret.offset) ?: return
        val classOfElement = element.findReferencedClass()?.qualifiedName ?: return

        invokeLater {
            runBackgroundableTask("Searching for Mixins", project, true) run@{ indicator ->
                indicator.isIndeterminate = true

                val classes = runReadAction {
                    val mixinAnnotation = JavaPsiFacade.getInstance(project).findClass(
                        MixinConstants.Annotations.MIXIN,
                        GlobalSearchScope.allScope(project)
                    ) ?: return@runReadAction null

                    // Check all classes with the Mixin annotation
                    val classes = AnnotatedElementsSearch.searchPsiClasses(
                        mixinAnnotation,
                        GlobalSearchScope.projectScope(project)
                    )
                        .filter {
                            indicator.text = "Checking ${it.name}..."

                            it.mixinTargets.any { c ->
                                c.qualifiedName == classOfElement
                            }
                        }

                    when (classes.size) {
                        0 -> null
                        1 -> classes
                        else ->
                            // Sort classes
                            classes.sortedBy(PsiClass::fullQualifiedName)
                    }
                } ?: return@run

                invokeLater {
                    if (classes.size == 1) {
                        gotoTargetElement(classes.single(), editor, file)
                    } else {
                        ToolWindowManager.getInstance(project).unregisterToolWindow(TOOL_WINDOW_ID)
                        val window = ToolWindowManager.getInstance(project)
                            .registerToolWindow(TOOL_WINDOW_ID, true, ToolWindowAnchor.BOTTOM)
                        window.icon = MixinAssets.MIXIN_CLASS_ICON

                        val component = FindMixinsComponent(classes)
                        val content = ContentFactory.SERVICE.getInstance().createContent(component.panel, null, false)
                        window.contentManager.addContent(content)

                        window.activate(null)
                    }
                }
            }
        }
    }
}
