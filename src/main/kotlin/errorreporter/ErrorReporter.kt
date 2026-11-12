/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginDetailsService
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import io.sentry.Attachment
import io.sentry.Hint
import io.sentry.SentryClient
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message
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

        val sentryEvent = SentryEvent()
        sentryEvent.serverName = ""
        sentryEvent.level = SentryLevel.ERROR
        additionalInfo?.let { sentryEvent.setExtra("additional_info", it) }

        event.message?.let { msg -> sentryEvent.message = Message().apply { message = msg} }

        sentryEvent.throwable = event.throwable

        PluginDetailsService.getInstance().findDetails(PluginUtil.PLUGIN_ID)?.version?.let { version ->
            val (ideaVersion, coreVersion) = version.split('-', limit = 2)
            sentryEvent.release = version
            sentryEvent.setTag("idea_version", ideaVersion)
            sentryEvent.setTag("core_version", coreVersion)
        }

        val appInfo = ApplicationInfoEx.getInstanceEx()
        val namesInfo = ApplicationNamesInfo.getInstance()

        sentryEvent.platform = "java"

        // Environment
        sentryEvent.setExtra("os", SystemInfo.OS_NAME)
        sentryEvent.setExtra("java_version", SystemInfo.JAVA_VERSION)
        sentryEvent.setExtra("java_vendor", SystemInfo.JAVA_VENDOR)

        // IDE
        sentryEvent.setExtra("ide", namesInfo.productName)
        sentryEvent.setExtra("ide_version", namesInfo.productName)
        sentryEvent.setExtra("ide_is_eap", appInfo.isEAP.toString())
        sentryEvent.setExtra("ide_build", appInfo.build.asString())
        sentryEvent.setExtra("ide_version", appInfo.fullVersion)

        sentryEvent.setExtra("last_action", IdeaLogger.ourLastActionId)

        val task = object : Task.Backgroundable(project, "Submitting error report", true) {
            override fun run(indicator: ProgressIndicator) {
                val opts = SentryOptions().apply { dsn = "https://e4e537f6f896904a3ec0586bce8c9a57@sentry.mcdev.io/1" }
                // Marked internal, but I have no idea what the intended way to do this, everything I can find online
                // only ever shows the global static API
                val client = SentryClient(opts)

                val hint = if (event.attachments.isNotEmpty()) {
                    Hint.withAttachments(event.attachments.map { Attachment(it.bytes, it.name) })
                } else {
                    null
                }

                val id = try {
                    client.captureEvent(sentryEvent, hint)
                } finally {
                    client.close()
                }

                // It would be nice to get the sentry issue associated with this event, but I don't see a direct way to
                // do that right now
                val message = "<html>${MCDevBundle("error_reporter.report.created", id.toString())}</html>"
                NotificationGroupManager.getInstance().getNotificationGroup("Error Report").createNotification(
                    MCDevBundle("error_reporter.report.title"),
                    message,
                    NotificationType.INFORMATION,
                ).setImportant(false).notify(project)

                val reportInfo = SubmittedReportInfo(null, "Event $id", SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
                consumer.consume(reportInfo)
            }
        }

        if (project == null) {
            task.run(EmptyProgressIndicator())
        } else {
            ProgressManager.getInstance().run(task)
        }

        return true
    }
}
