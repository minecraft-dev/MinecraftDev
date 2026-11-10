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
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterNotifications
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterPullRequests
import com.demonwav.mcdev.platform.fabric.annotater.git.AnnotaterGitVcs
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocContributions
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.io.HttpRequests
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHCompatibilityUtil

/**
 * Commits annotater changes, forks annotater on GitHub, pushes the contribution branch to the fork and opens a pull
 * request, using accounts from the GitHub plugin.
 */
class AnnotaterGitHubPullRequests : AnnotaterPullRequests {
    override fun canCreatePullRequest(checkout: AnnotaterCheckout) =
        checkout.managed && AnnotaterGitVcs.instance != null

    override fun createPullRequest(project: Project, checkout: AnnotaterCheckout) {
        val git = AnnotaterGitVcs.instance ?: return

        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.pr.checking"), true) {
            private var changes = emptyList<String>()
            private var uncommittedChanges = emptyList<String>()
            private var commitsAhead = 0
            private var branch: String? = null
            private var existingTitle: String? = null

            override fun run(indicator: ProgressIndicator) {
                changes = git.mappingFilesChangedSinceUpstream(project, checkout)
                uncommittedChanges = git.uncommittedMappingFiles(project, checkout)
                commitsAhead = git.commitsAheadOfUpstream(project, checkout)
                branch = git.currentContribution(checkout)?.branch
                existingTitle = branch?.let { git.pullRequestTitle(project, checkout, it) }
            }

            override fun onSuccess() {
                val branch = branch
                if (changes.isEmpty() && uncommittedChanges.isEmpty() && commitsAhead == 0 || branch == null) {
                    AnnotaterNotifications.notifyInfo(
                        project,
                        MCDevBundle("fabric.annotater.pr.nothing"),
                        MCDevBundle("fabric.annotater.pr.nothing.content", checkout.version),
                    )
                    return
                }
                showDialog(project, checkout, git, branch, changes, uncommittedChanges, existingTitle)
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.pr.failed"), error)
            }
        }.queue()
    }

    private fun showDialog(
        project: Project,
        checkout: AnnotaterCheckout,
        git: AnnotaterGitVcs,
        branch: String,
        changes: List<String>,
        uncommittedChanges: List<String>,
        existingTitle: String?,
    ) {
        if (GHAccountsUtil.accounts.isEmpty()) {
            GHAccountsUtil.requestNewAccount(GithubServerPath.DEFAULT_SERVER, null, project) ?: return
        }
        val accounts = GHAccountsUtil.accounts.filter { it.server.isGithubDotCom }
        if (accounts.isEmpty()) {
            AnnotaterNotifications.notifyInfo(
                project,
                MCDevBundle("fabric.annotater.pr.failed"),
                MCDevBundle("fabric.annotater.pr.no_account"),
            )
            return
        }
        val defaultAccount = GHAccountsUtil.getSingleOrDefaultAccount(project)?.takeIf { it in accounts }

        val dialog =
            AnnotaterPullRequestDialog(project, accounts, defaultAccount, changes, uncommittedChanges, existingTitle)
        if (!dialog.showAndGet()) {
            return
        }
        val account = dialog.account ?: return
        val token = GHCompatibilityUtil.getOrRequestToken(account, project) ?: return

        val request = PullRequest(dialog.prTitle, dialog.prDescription, dialog.commitMessage, branch)
        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.pr.creating"), false) {
            private var url: String? = null

            override fun run(indicator: ProgressIndicator) {
                url =
                    submit(project, checkout, git, account, token, request, uncommittedChanges.isNotEmpty(), indicator)
                // Label the contribution with its pull request, for switching back to it later
                runCatching { git.setContributionTitle(project, checkout, request.branch, request.title) }
            }

            override fun onSuccess() {
                val url = url ?: return
                Notification(
                    AnnotaterNotifications.GROUP_ID,
                    MCDevBundle("fabric.annotater.pr.created"),
                    url,
                    NotificationType.INFORMATION,
                ).addAction(
                    NotificationAction.createSimple(MCDevBundle("fabric.annotater.pr.open")) {
                        BrowserUtil.browse(url)
                    },
                ).addAction(
                    NotificationAction.createSimpleExpiring(MCDevBundle("fabric.annotater.pr.start_new")) {
                        AnnotaterJavadocContributions.switchTo(project, checkout, null)
                    },
                ).notify(project)
                BrowserUtil.browse(url)
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.pr.failed"), error)
            }
        }.queue()
    }

    private class PullRequest(val title: String, val body: String, val commitMessage: String, val branch: String)

    @Throws(IOException::class, VcsException::class)
    private fun submit(
        project: Project,
        checkout: AnnotaterCheckout,
        git: AnnotaterGitVcs,
        account: GithubAccount,
        token: String,
        request: PullRequest,
        hasChanges: Boolean,
        indicator: ProgressIndicator,
    ): String {
        val api = GitHubApi(account.server.toApiUrl(), token)
        val upstream = AnnotaterGitVcs.upstreamRepository

        // Otherwise GitHub only rejects the pull request with "base invalid" after committing, forking and pushing
        if (!api.exists("/repos/$upstream/branches/${checkout.version}")) {
            throw IOException(MCDevBundle("fabric.annotater.pr.no_base_branch", upstream, checkout.version))
        }

        if (hasChanges) {
            indicator.text = MCDevBundle("fabric.annotater.pr.committing")
            git.commitMappings(project, checkout, request.commitMessage)
        }

        indicator.text = MCDevBundle("fabric.annotater.pr.forking")
        val fork = api.post("/repos/$upstream/forks", JsonObject())
        val forkName = fork.get("full_name").asString
        val forkOwner = fork.getAsJsonObject("owner").get("login").asString
        val cloneUrl = fork.get("clone_url").asString
        waitForFork(api, forkName, indicator)

        indicator.text = MCDevBundle("fabric.annotater.pr.pushing")
        git.push(project, checkout, cloneUrl, request.branch)

        indicator.text = MCDevBundle("fabric.annotater.pr.opening")
        val body = JsonObject().apply {
            addProperty("title", request.title)
            addProperty("body", request.body)
            addProperty("head", "$forkOwner:${request.branch}")
            addProperty("base", checkout.version)
            addProperty("maintainer_can_modify", true)
        }
        return try {
            api.post("/repos/$upstream/pulls", body).get("html_url").asString
        } catch (e: GitHubApiException) {
            // The branch was pushed to an existing pull request, which is now up to date
            val head = "$forkOwner:${request.branch}"
            val existing = if (e.statusCode == HTTP_UNPROCESSABLE_ENTITY) {
                api.get("/repos/$upstream/pulls?state=open&head=${URLEncoder.encode(head, Charsets.UTF_8)}")
                    .asJsonArray.firstOrNull()?.asJsonObject?.get("html_url")?.asString
            } else {
                null
            }
            existing ?: throw e
        }
    }

    /**
     * Forks are created asynchronously, so wait until GitHub has finished creating it before pushing.
     */
    private fun waitForFork(api: GitHubApi, forkName: String, indicator: ProgressIndicator) {
        repeat(FORK_POLL_ATTEMPTS) {
            indicator.checkCanceled()
            if (api.exists("/repos/$forkName")) {
                return
            }
            Thread.sleep(FORK_POLL_DELAY_MS)
        }
    }

    private class GitHubApiException(val statusCode: Int, message: String) : IOException(message)

    private class GitHubApi(private val baseUrl: String, private val token: String) {
        private val gson = Gson()

        fun post(path: String, body: JsonObject): JsonObject {
            return HttpRequests.post(baseUrl + path, "application/json")
                .tuner(::authorize)
                .throwStatusCodeException(false)
                .connect { request ->
                    request.write(gson.toJson(body))
                    gson.fromJson(readResponse(request), JsonObject::class.java)
                }
        }

        fun get(path: String): JsonElement {
            return HttpRequests.request(baseUrl + path)
                .tuner(::authorize)
                .throwStatusCodeException(false)
                .connect { request -> JsonParser.parseString(readResponse(request)) }
        }

        fun exists(path: String): Boolean {
            return try {
                get(path)
                true
            } catch (e: GitHubApiException) {
                if (e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) false else throw e
            }
        }

        /**
         * Reads the response body, or throws an exception with GitHub's explanation of the error, which
         * [HttpURLConnection] otherwise hides.
         */
        private fun readResponse(request: HttpRequests.Request): String {
            val connection = request.connection as HttpURLConnection
            val statusCode = connection.responseCode
            if (statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                return request.readString()
            }
            val error = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            throw GitHubApiException(statusCode, describeError(statusCode, error))
        }

        private fun describeError(statusCode: Int, body: String): String {
            val json = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
                ?: return "GitHub returned HTTP $statusCode: $body"
            val message = json.get("message")?.takeIf { it.isJsonPrimitive }?.asString ?: "HTTP $statusCode"
            val details = json.get("errors")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { error ->
                when {
                    error.isJsonPrimitive -> error.asString
                    error.isJsonObject -> {
                        val obj = error.asJsonObject
                        obj.get("message")?.asString
                            ?: listOfNotNull(obj.get("field")?.asString, obj.get("code")?.asString)
                                .joinToString(" ")
                                .takeIf { it.isNotEmpty() }
                    }

                    else -> null
                }
            }.orEmpty()
            return if (details.isEmpty()) message else "$message: ${details.joinToString("; ")}"
        }

        private fun authorize(connection: java.net.URLConnection) {
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
    }

    companion object {
        private const val FORK_POLL_ATTEMPTS = 30
        private const val FORK_POLL_DELAY_MS = 2000L
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}
