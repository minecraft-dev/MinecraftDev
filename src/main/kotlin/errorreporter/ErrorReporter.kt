/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.errorreporter

import com.demonwav.mcdev.update.PluginUtil
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ReportMessages
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.Consumer
import java.awt.Component

class ErrorReporter : ErrorReportSubmitter() {
    private val baseUrl = "https://github.com/minecraft-dev/MinecraftDev/issues"
    override fun getReportActionText() = "Report to Minecraft Dev GitHub Issue Tracker"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        val event = events[0]
        val errorData = ErrorData(event.throwable, IdeaLogger.ourLastActionId)
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)

        errorData.description = additionalInfo
        errorData.message = event.message

        PluginManagerCore.getPlugin(PluginUtil.PLUGIN_ID)?.let { plugin ->
            errorData.pluginName = plugin.name
            errorData.pluginVersion = plugin.version
        }

        val data = event.data

        if (data is LogMessage) {
            errorData.throwable = data.throwable
            errorData.attachments = data.includedAttachments
        }

        val (reportValues, attachments) = errorData.formatErrorData()

        val project = CommonDataKeys.PROJECT.getData(dataContext)

        val task = AnonymousFeedbackTask(
            project,
            "Submitting error report",
            true,
            reportValues,
            attachments,
            { htmlUrl, token, isDuplicate ->
                val type = if (isDuplicate) {
                    SubmittedReportInfo.SubmissionStatus.DUPLICATE
                } else {
                    SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
                }

                val message = if (!isDuplicate) {
                    "<html>Created Issue #$token successfully. <a href=\"$htmlUrl\">View issue.</a></html>"
                } else {
                    "<html>Commented on existing Issue #$token successfully. " +
                        "<a href=\"$htmlUrl\">View comment.</a></html>"
                }

                ReportMessages.GROUP.createNotification(
                    ReportMessages.getErrorReport(),
                    message,
                    NotificationType.INFORMATION,
                    NotificationListener.URL_OPENING_LISTENER
                ).setImportant(false).notify(project)

                val reportInfo = SubmittedReportInfo(htmlUrl, "Issue #$token", type)
                consumer.consume(reportInfo)
            },
            { e ->
                val message = "<html>Error Submitting Issue: ${e.message}<br>Consider opening an issue on " +
                    "<a href=\"$baseUrl\">the GitHub issue tracker.</a></html>"
                ReportMessages.GROUP.createNotification(
                    ReportMessages.getErrorReport(),
                    message,
                    NotificationType.ERROR,
                    NotificationListener.URL_OPENING_LISTENER
                ).setImportant(false).notify(project)

                consumer.consume(SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.FAILED))
            }
        )

        if (project == null) {
            task.run(EmptyProgressIndicator())
        } else {
            ProgressManager.getInstance().run(task)
        }

        return true
    }
}
