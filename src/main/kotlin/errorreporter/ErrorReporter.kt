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

package com.demonwav.mcdev.errorreporter

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.update.PluginUtil
import com.intellij.diagnostic.LogMessage
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import errorreporter.submission.Submission
import errorreporter.submission.SubmissionAttachment
import errorreporter.submission.SubmissionContent
import errorreporter.submission.SubmissionMetadata
import java.awt.Component
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class ErrorReporter : ErrorReportSubmitter() {

    override fun getReportActionText() = MCDevBundle("error_reporter.submit.action")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)

        val plugin = PluginUtil.plugin
        val appInfo = ApplicationInfoEx.getInstanceEx()
        val namesInfo = ApplicationNamesInfo.getInstance()

        val meta = SubmissionMetadata(
            pluginName = plugin.name,
            pluginVersion = plugin.version,
            osName = SystemInfo.OS_NAME,
            javaVersion = SystemInfo.JAVA_VERSION,
            javaVmVendor = SystemInfo.JAVA_VENDOR,
            isEap = appInfo.isEAP,
            appName = namesInfo.fullProductName,
            ideaBuild = appInfo.build.toString(),
            ideaVersion = appInfo.fullVersion,
            lastAction = IdeaLogger.ourLastActionId
        )

        val errorDetails = events.map { event ->
            SubmissionContent(
                message = event.message,
                description = additionalInfo,
                stacktrace = event.throwableText,
                attachments = (event.data as? LogMessage)?.let { msg ->
                    msg.includedAttachments.map { attachment ->
                        val text = runCatching {
                            Charsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(attachment.bytes))
                                .toString()
                        }.getOrNull()

                        val bodyText = text?.takeIf { it != attachment.displayText }
                        val bytes = if (text == null) attachment.encodedBytes else null
                        SubmissionAttachment(
                            name = attachment.name,
                            displayText = attachment.displayText,
                            body = bodyText,
                            bytes = bytes
                        )
                    }
                } ?: listOf()
            )
        }

        val submission = Submission(meta, errorDetails)

        val task = ErrorReporterTask(
            project,
            "Submitting error report",
            true,
            submission,
            consumer,
        )

        if (project == null) {
            task.run(EmptyProgressIndicator())
        } else {
            ProgressManager.getInstance().run(task)
        }

        return true
    }
}
