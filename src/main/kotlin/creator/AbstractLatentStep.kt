/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.invokeLater
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.AsyncProcessIcon
import java.awt.event.HierarchyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

abstract class AbstractLatentStep<T>(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    companion object {
        private val LOGGER = logger<AbstractLatentStep<*>>()
    }

    private var hasComputedData = false
    private var step: NewProjectWizardStep? = null

    protected abstract val description: String

    private fun doComputeData(placeholder: Placeholder) {
        if (hasComputedData) {
            return
        }
        hasComputedData = true
        CoroutineScope(Dispatchers.Swing).launch {
            val result = asyncIO {
                try {
                    computeData()
                } catch (e: Throwable) {
                    LOGGER.error(e)
                    null
                }
            }.await()
            invokeLater {
                if (result == null) {
                    placeholder.component = JBLabel("Unable to $description").also { it.foreground = JBColor.RED }
                } else {
                    val s = createStep(result)
                    step = s
                    val panel = panel {
                        s.setupUI(this)
                    }
                    placeholder.component = panel
                }
            }
        }
    }

    protected abstract suspend fun computeData(): T?

    protected abstract fun createStep(data: T): NewProjectWizardStep

    override fun setupUI(builder: Panel) {
        lateinit var placeholder: Placeholder
        with(builder) {
            row {
                placeholder = placeholder()
            }
        }
        placeholder.component = panel {
            row(description.capitalize()) {
                cell(AsyncProcessIcon("${javaClass}.computeData").also { component ->
                    component.addHierarchyListener { event ->
                        if ((event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && component.isShowing) {
                            doComputeData(placeholder)
                        }
                    }
                })
            }
        }
    }

    override fun setupProject(project: Project) {
        step?.setupProject(project)
    }
}