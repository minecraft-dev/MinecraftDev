/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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


package com.demonwav.mcdev.platform.fabric.annotater.github

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckout
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Font

/**
 * Runs annotater's own checks with `gradlew check` before a pull request is submitted.
 */
object AnnotaterBuildCheck {
    /**
     * Runs the checks in [checkout] with a modal progress dialog. If they fail, the user is shown the output and can
     * choose to submit anyway.
     *
     * @return whether to continue submitting
     */
    fun run(project: Project, checkout: AnnotaterCheckout): Boolean {
        var result: ProcessOutput? = null
        var error: Throwable? = null
        // Modal tasks finish before this returns
        ProgressManager.getInstance().run(
            object : Task.Modal(project, MCDevBundle("fabric.annotater.pr.check_build.progress"), true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    result = CapturingProcessHandler(commandLine(project, checkout))
                        .runProcessWithProgressIndicator(indicator)
                }

                override fun onThrowable(e: Throwable) {
                    error = e
                }
            },
        )
        error?.let { return FailedDialog(project, it.message ?: it.toString()).showAndGet() }
        val output = result?.takeUnless { it.isCancelled } ?: return false
        if (output.exitCode == 0 && !output.isTimeout) {
            return true
        }
        val log = (output.stdout + "\n" + output.stderr).trim()
        return FailedDialog(project, log).showAndGet()
    }

    private fun commandLine(project: Project, checkout: AnnotaterCheckout): GeneralCommandLine {
        val wrapper = checkout.root.resolve(if (SystemInfo.isWindows) "gradlew.bat" else "gradlew")
        val commandLine = GeneralCommandLine(wrapper.toString(), "check", "--console=plain")
            .withWorkingDirectory(checkout.root)
            .withCharset(Charsets.UTF_8)
        // Minecraft projects need a recent Java version, which the environment's JAVA_HOME may not be
        val sdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType is JavaSdkType }
        sdk?.homePath?.let { commandLine.withEnvironment("JAVA_HOME", it) }
        return commandLine
    }

    private class FailedDialog(project: Project, private val log: String) : DialogWrapper(project) {
        init {
            title = MCDevBundle("fabric.annotater.pr.check_build.failed")
            setOKButtonText(MCDevBundle("fabric.annotater.pr.check_build.submit_anyway"))
            okAction.putValue(DEFAULT_ACTION, null)
            cancelAction.putValue(DEFAULT_ACTION, true)
            init()
        }

        override fun createCenterPanel() = panel {
            row {
                label(MCDevBundle("fabric.annotater.pr.check_build.failed.message"))
            }
            row {
                val textArea = JBTextArea(log).apply {
                    isEditable = false
                    font = JBUI.Fonts.create(Font.MONOSPACED, font.size)
                    // The cause of a failure is usually at the end
                    caretPosition = document.length
                }
                cell(JBScrollPane(textArea))
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
                    .applyToComponent { preferredSize = Dimension(JBUI.scale(800), JBUI.scale(400)) }
            }.resizableRow()
        }
    }
}
