/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.awt.Color
import java.util.LinkedHashMap
import javax.swing.Icon
import org.apache.commons.lang.builder.ToStringBuilder
import org.jetbrains.annotations.Contract

abstract class AbstractModuleType<out T : AbstractModule>(val groupId: String, val artifactId: String) {

    val colorMap = LinkedHashMap<String, Color>()

    abstract val platformType: PlatformType

    abstract val icon: Icon?

    open val hasIcon = true

    abstract val id: String

    abstract val ignoredAnnotations: List<String>

    abstract val listenerAnnotations: List<String>

    val classToColorMappings: Map<String, Color>
        get() = this.colorMap

    abstract fun generateModule(facet: MinecraftFacet): T

    fun performCreationSettingSetup(project: Project) {
        if (project.isDisposed) {
            return
        }
        val manager = EntryPointsManager.getInstance(project)
        val annotations = (manager as? EntryPointsManagerBase)?.ADDITIONAL_ANNOTATIONS as? MutableList<String> ?: return
        ignoredAnnotations.asSequence()
            .filter { annotation -> !annotations.contains(annotation) }
            .forEach { annotations.add(it) }
    }

    open fun getEventGenerationPanel(chosenClass: PsiClass): EventGenerationPanel {
        return EventGenerationPanel(chosenClass)
    }

    @get:Contract(pure = true)
    open val isEventGenAvailable: Boolean
        get() = false

    open fun getDefaultListenerName(psiClass: PsiClass) = "on" + psiClass.name!!.replace("Event", "")

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
