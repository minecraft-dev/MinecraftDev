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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.awt.Color
import javax.swing.Icon
import org.apache.commons.lang3.builder.ToStringBuilder

abstract class AbstractModuleType<out T : AbstractModule>(val groupId: String, val artifactId: String) {

    val colorMap = LinkedHashMap<String, Color>()

    abstract val platformType: PlatformType

    abstract val icon: Icon?

    open val hasIcon = true
    open val isIconSecondary = false

    abstract val id: String

    abstract val ignoredAnnotations: List<String>

    abstract val listenerAnnotations: List<String>

    open fun classToColorMappings(module: Module): Map<String, Color> = this.colorMap

    abstract fun generateModule(facet: MinecraftFacet): T

    fun performCreationSettingSetup(project: Project) {
        if (project.isDisposed) {
            return
        }
        val manager = EntryPointsManager.getInstance(project)
        val annotations = (manager as? EntryPointsManagerBase)?.customAdditionalAnnotations?.toMutableList() ?: return
        ignoredAnnotations.asSequence()
            .filter { annotation -> !annotations.contains(annotation) }
            .forEach { annotations.add(it) }
    }

    open fun getEventGenerationPanel(chosenClass: PsiClass): EventGenerationPanel {
        return EventGenerationPanel(chosenClass)
    }

    open val isEventGenAvailable: Boolean
        get() = false

    open fun getDefaultListenerName(psiClass: PsiClass) = "on" + psiClass.name?.replace("Event", "")

    override fun toString(): String {
        return ToStringBuilder(this)
            .append("groupId", groupId)
            .append("artifactId", artifactId)
            .toString()
    }

    /**
     * Given any PsiElement, determine if it resides in a module of this [AbstractModuleType].

     * @param element The element to check.
     * *
     * @return True if this element resides in a module of this type
     */
    fun isInModule(element: PsiElement): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false

        val facet = MinecraftFacet.getInstance(module)
        return facet != null && facet.isOfType(this)
    }

    protected fun defaultNameForSubClassEvents(psiClass: PsiClass): String {
        val isInnerClass = psiClass.parent !is PsiFile

        val name = StringBuilder()
        if (isInnerClass) {
            val containingClass = psiClass.parent.findContainingClass()
            if (containingClass != null && containingClass.name != null) {
                name.append(containingClass.name!!.replace("Event", ""))
            }
        }

        var className = psiClass.name!!
        if (className.startsWith(name.toString())) {
            className = className.substring(name.length)
        }
        name.append(className.replace("Event", ""))

        name.insert(0, "on")
        return name.toString()
    }
}
