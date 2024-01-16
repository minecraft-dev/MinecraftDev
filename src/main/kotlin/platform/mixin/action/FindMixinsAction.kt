/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.cached
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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.ui.content.ContentFactory

class FindMixinsAction : AnAction() {

    class TWFactory : ToolWindowFactory {
        override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        }
    }

    companion object {
        private const val TOOL_WINDOW_ID = "Find Mixins"

        fun findMixins(
            clazz: PsiClass,
            project: Project,
            indicator: ProgressIndicator? = null,
        ): List<PsiClass>? {
            return clazz.cached(PsiModificationTracker.MODIFICATION_COUNT) {
                val targetInternalName = clazz.fullQualifiedName?.replace('.', '/')
                    ?: return@cached null

                val mixinAnnotation = JavaPsiFacade.getInstance(project).findClass(
                    MixinConstants.Annotations.MIXIN,
                    GlobalSearchScope.allScope(project),
                ) ?: return@cached null

                // Check all classes with the Mixin annotation
                val classes = AnnotatedElementsSearch.searchPsiClasses(
                    mixinAnnotation,
                    GlobalSearchScope.projectScope(project),
                )
                    .filter {
                        indicator?.text = "Checking ${it.name}..."

                        it.mixinTargets.any { c ->
                            c.name == targetInternalName
                        }
                    }

                classes
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PROJECT) ?: return
        val file = e.getData(PSI_FILE) ?: return
        val caret = e.getData(CARET) ?: return
        val editor = e.getData(EDITOR) ?: return

        val element = file.findElementAt(caret.offset) ?: return
        val classOfElement = element.findReferencedClass() ?: return

        invokeLater {
            runBackgroundableTask("Searching for Mixins", project, true) run@{ indicator ->
                indicator.isIndeterminate = true

                val classes = runReadAction {
                    if (!classOfElement.isValid) {
                        return@runReadAction null
                    }

                    val classes = findMixins(classOfElement, project, indicator) ?: return@runReadAction null

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
                        val twManager = ToolWindowManager.getInstance(project)
                        val window = twManager.getToolWindow(TOOL_WINDOW_ID)!!
                        val component = FindMixinsComponent(classes)
                        val content = ContentFactory.getInstance().createContent(component.panel, null, false)
                        content.displayName = classOfElement.qualifiedName ?: classOfElement.name
                        window.contentManager.addContent(content)

                        window.activate(null)
                    }
                }
            }
        }
    }
}
