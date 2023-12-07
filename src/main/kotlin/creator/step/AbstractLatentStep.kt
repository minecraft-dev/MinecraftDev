/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.onHidden
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.AsyncProcessIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/**
 * Used for when a long-running task is required to fully construct the wizard steps, for example when downloading
 * Minecraft versions.
 */
abstract class AbstractLatentStep<T>(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    companion object {
        private val LOGGER = logger<AbstractLatentStep<*>>()
    }

    private var hasComputedData = false
    private var step: NewProjectWizardStep? = null

    /**
     * Description of this step displayed to the user.
     *
     * This should be in sentence case starting with a lower case letter, and starting with a verb in the present tense,
     * like a Git commit message.
     *
     * For example, "download Minecraft versions" would be an appropriate description.
     */
    protected abstract val description: String

    private fun doComputeData(placeholder: Placeholder, lifetime: Disposable) {
        if (hasComputedData) {
            return
        }
        hasComputedData = true

        var disposed = false
        Disposer.register(lifetime) {
            hasComputedData = false
            disposed = true
        }

        CoroutineScope(Dispatchers.Swing).launch {
            if (disposed) {
                return@launch
            }

            val (result: T?, errorMessage: String?) = asyncIO {
                try {
                    computeData() to null
                } catch (e: Throwable) {
                    LOGGER.warn("computeData failed", e)
                    null to e.message
                }
            }.await()

            if (disposed) {
                return@launch
            }

            invokeLater {
                if (disposed) {
                    return@invokeLater
                }

                if (result == null) {
                    placeholder.component = panel {
                        row {
                            val labelValidationText =
                                MCDevBundle("creator.ui.generic_validation_failure.message", description, errorMessage)
                            val label = label(labelValidationText)
                                .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                                .validation(DialogValidation { ValidationInfo(labelValidationText) })
                            label.component.foreground = JBColor.RED
                        }
                    }
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
                cell(
                    AsyncProcessIcon("$javaClass.computeData").also { component ->
                        var lifetime: Disposable? = null
                        component.onShown {
                            lifetime?.let(Disposer::dispose)
                            lifetime = Disposer.newDisposable().also { lifetime ->
                                Disposer.register(context.disposable, lifetime)
                                doComputeData(placeholder, lifetime)
                            }
                        }
                        component.onHidden {
                            lifetime?.let(Disposer::dispose)
                            lifetime = null
                        }
                    },
                )
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                    .validation(
                        DialogValidation {
                            ValidationInfo(MCDevBundle("creator.ui.generic_unfinished.message", description))
                        }
                    )
            }
        }
    }

    override fun setupProject(project: Project) {
        step?.setupProject(project)
    }
}
