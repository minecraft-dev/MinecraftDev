/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.error

import com.demonwav.mcdev.update.PluginUtil
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ReportMessages
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
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
        val bean = ErrorBean(event.throwable, IdeaLogger.ourLastActionId)
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)

        bean.description = additionalInfo
        bean.message = event.message

        PluginManager.getPlugin(PluginUtil.PLUGIN_ID)?.let { plugin ->
            bean.pluginName = plugin.name
            bean.pluginVersion = plugin.version
        }

        val data = event.data

        if (data is LogMessage) {
            bean.attachments = data.includedAttachments
        }

        val (reportValues, attachments) =
            IdeaITNProxy.getKeyValuePairs(bean, ApplicationInfoEx.getInstanceEx(), ApplicationNamesInfo.getInstance())

        val project = CommonDataKeys.PROJECT.getData(dataContext)

        val task = AnonymousFeedbackTask(
            project, "Submitting error report", true, reportValues, attachments,
            { htmlUrl, token, isDuplicate ->
                val reportInfo = SubmittedReportInfo(htmlUrl, "Issue #$token", SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
                consumer.consume(reportInfo)

                val message = if (!isDuplicate) {
                    "<html>Created Issue #$token successfully. " +
                        "<a href=\"$htmlUrl\">View issue.</a></html>"
                } else {
                    "<html>Commented on existing Issue #$token successfully. " +
                        "<a href=\"$htmlUrl\">View comment.</a></html>"
                }

                ReportMessages.GROUP.createNotification(
                    ReportMessages.ERROR_REPORT,
                    message,
                    NotificationType.INFORMATION,
                    NotificationListener.URL_OPENING_LISTENER
                ).setImportant(false).notify(project)
            },
            { e ->
                val message = "<html>Error Submitting Issue: ${e.message}<br>Consider opening an issue on " +
                    "<a href=\"$baseUrl\">the GitHub issue tracker.</a></html>"
                ReportMessages.GROUP.createNotification(
                    ReportMessages.ERROR_REPORT,
                    message,
                    NotificationType.ERROR,
                    NotificationListener.URL_OPENING_LISTENER
                ).setImportant(false).notify(project)
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
