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
import com.demonwav.mcdev.errorreporter.submission.SubmissionResponseError
import com.demonwav.mcdev.errorreporter.submission.SubmissionResponseSuccess
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.HttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import com.intellij.util.io.HttpRequests
import errorreporter.submission.Submission
import java.net.HttpURLConnection
import org.apache.http.HttpStatus

private const val SUBMISSION_URL: String = "https://github.mcdev.io/api/v1/submit"

class ErrorReporterTask(
    project: Project?,
    title: String,
    canBeCancelled: Boolean,
    private val submission: Submission,
    private val consumer: Consumer<in SubmittedReportInfo>,
) : Task.Backgroundable(project, title, canBeCancelled) {

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true

        try {
            val gson = Gson()
            val requestText = gson.toJson(submission).toByteArray(Charsets.UTF_8)

            var status: Int = -1
            val responseText = HttpRequests.post(SUBMISSION_URL, "application/json; charset=utf-8")
                .userAgent(userAgent)
                .forceHttps(true)
                .throwStatusCodeException(false)
                .connect { request ->
                    request.write(requestText)
                    status = (request.connection as HttpURLConnection).responseCode
                    request.readString(indicator)
                }

            val response = if (status == HttpStatus.SC_OK) {
                gson.fromJson<SubmissionResponseSuccess>(responseText)
            } else {
                gson.fromJson<SubmissionResponseError>(responseText)
            }

            when (response) {
                is SubmissionResponseSuccess -> handleSuccess(response)
                is SubmissionResponseError -> handleError(response)
            }
        } catch (e: SubmissionResponseErrorException) {
            handleError(e.response)
        } catch (e: Exception) {
            handleError(SubmissionResponseError((e.message ?: "Unknown error")))
        }
    }

    private fun handleSuccess(response: SubmissionResponseSuccess) {
        val type = if (response.isDuplicate) {
            SubmittedReportInfo.SubmissionStatus.DUPLICATE
        } else {
            SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
        }

        val message = if (!response.isDuplicate) {
            "<html>${MCDevBundle("error_reporter.report.created", response.issueNumber)}</html>"
        } else {
            "<html>${MCDevBundle("error_reporter.report.commented", response.issueNumber)}</html>"
        }
        val actionText = if (!response.isDuplicate) {
            MCDevBundle("error_reporter.report.created.action")
        } else {
            MCDevBundle("error_reporter.report.commented.action")
        }

        NotificationGroupManager.getInstance().getNotificationGroup("Error Report").createNotification(
            MCDevBundle("error_reporter.report.title"),
            message,
            NotificationType.INFORMATION,
        ).addAction(BrowseNotificationAction(actionText, response.issueUrl)).setImportant(false).notify(project)

        val reportInfo = SubmittedReportInfo(response.issueUrl, "Issue #${response.issueNumber}", type)
        consumer.consume(reportInfo)
    }

    private fun handleError(response: SubmissionResponseError) {
        val message = "<html>${MCDevBundle("error_reporter.report.error", response.message)}</html>"
        val actionText = MCDevBundle("error_reporter.report.error.action")
        val userUrl = "https://github.com/minecraft-dev/MinecraftDev/issues"
        NotificationGroupManager.getInstance().getNotificationGroup("Error Report").createNotification(
            MCDevBundle("error_reporter.report.title"),
            message,
            NotificationType.ERROR,
        ).addAction(BrowseNotificationAction(actionText, userUrl)).setImportant(false).notify(project)

        consumer.consume(SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.FAILED))
    }

    private fun connect(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        val connection = factory.openHttpConnection(url)
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection
    }

    private companion object {
        private val userAgent by lazy {
            var agent = "Minecraft Development for IntelliJ"

            val pluginDescription = PluginManagerCore.getPlugin(PluginUtil.PLUGIN_ID)
            if (pluginDescription != null) {
                val name = pluginDescription.name
                val version = pluginDescription.version
                agent = "$name ($version)"
            }
            agent
        }
    }
}

private class SubmissionResponseErrorException(val response: SubmissionResponseError) : Exception()
