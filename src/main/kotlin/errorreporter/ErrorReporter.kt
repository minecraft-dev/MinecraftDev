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
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.IdeaLogger
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.util.Consumer
import java.awt.Component

class ErrorReporter : ErrorReportSubmitter() {
    private val ignoredErrorMessages = listOf(
        "Key com.demonwav.mcdev.translations.TranslationFoldingSettings duplicated",
        "Inspection #EntityConstructor has no description",
        "VFS name enumerator corrupted",
        "PersistentEnumerator storage corrupted",
    )
    override fun getReportActionText() = MCDevBundle("error_reporter.submit.action")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)

        val event = events[0]
        val errorMessage = event.throwableText
        if (errorMessage.isNotBlank() && ignoredErrorMessages.any(errorMessage::contains)) {
            val task = object : Task.Backgroundable(project, MCDevBundle("error_reporter.submit.ignored")) {
                override fun run(indicator: ProgressIndicator) {
                    consumer.consume(SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.DUPLICATE))
                }
            }
            if (project == null) {
                task.run(EmptyProgressIndicator())
            } else {
                ProgressManager.getInstance().run(task)
            }
            return true
        }

        val errorData = ErrorData(event.throwable, IdeaLogger.ourLastActionId)

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
                    "<html>${MCDevBundle("error_reporter.report.created", token)}</html>"
                } else {
                    "<html>${MCDevBundle("error_reporter.report.commented", token)}</html>"
                }
                val actionText = if (!isDuplicate) {
                    MCDevBundle("error_reporter.report.created.action")
                } else {
                    MCDevBundle("error_reporter.report.commented.action")
                }

                NotificationGroupManager.getInstance().getNotificationGroup("Error Report").createNotification(
                    MCDevBundle("error_reporter.report.title"),
                    message,
                    NotificationType.INFORMATION,
                ).addAction(BrowseNotificationAction(actionText, htmlUrl)).setImportant(false).notify(project)

                val reportInfo = SubmittedReportInfo(htmlUrl, "Issue #$token", type)
                consumer.consume(reportInfo)
            },
            { e ->
                val message = "<html>${MCDevBundle("error_reporter.report.error", e.message)}</html>"
                val actionText = MCDevBundle("error_reporter.report.error.action")
                val userUrl = "https://github.com/minecraft-dev/MinecraftDev/issues"
                NotificationGroupManager.getInstance().getNotificationGroup("Error Report").createNotification(
                    MCDevBundle("error_reporter.report.title"),
                    message,
                    NotificationType.ERROR,
                ).addAction(BrowseNotificationAction(actionText, userUrl)).setImportant(false).notify(project)

                consumer.consume(SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.FAILED))
            },
        )

        if (project == null) {
            task.run(EmptyProgressIndicator())
        } else {
            ProgressManager.getInstance().run(task)
        }

        return true
    }
}
