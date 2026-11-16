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
import com.demonwav.mcdev.util.McdevHttp
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
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHCompatibilityUtil

class AnnotaterGitHubPullRequests : AnnotaterPullRequests {
    /**
     * The states of pull requests by URL, cached so that the editor banner can show them without blocking.
     */
    private val states = ConcurrentHashMap<String, CachedState>()

    private val pendingStates = ConcurrentHashMap.newKeySet<String>()

    private val stateExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Annotater pull request state", 1)

    private class CachedState(val state: AnnotaterPullRequests.State?, val time: Long)

    override fun availableAction(checkout: AnnotaterCheckout): AnnotaterPullRequests.Action? {
        AnnotaterGitVcs.instance?.pushStatus(checkout)?.takeIf { it.hasUnpushedChanges } ?: return null
        val pullRequest = currentPullRequest(checkout)
        return if (pullRequest != null && !pullRequest.closed) {
            AnnotaterPullRequests.Action.UPDATE
        } else {
            AnnotaterPullRequests.Action.CREATE
        }
    }

    override fun currentPullRequest(checkout: AnnotaterCheckout): AnnotaterPullRequests.PullRequest? {
        val url = AnnotaterGitVcs.instance?.pushStatus(checkout)?.pullRequestUrl ?: return null
        val cached = states[url]
        if ((cached == null || System.currentTimeMillis() - cached.time > STATE_CACHE_MS) && pendingStates.add(url)) {
            stateExecutor.execute {
                try {
                    lookUpState(url)
                } finally {
                    pendingStates.remove(url)
                }
            }
        }
        return AnnotaterPullRequests.PullRequest(url, cached?.state)
    }

    override fun submit(project: Project, checkout: AnnotaterCheckout) {
        val git = AnnotaterGitVcs.instance ?: return

        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.pr.checking"), true) {
            private var changes = emptyList<String>()
            private var uncommittedChanges = emptyList<String>()
            private var commitsAhead = 0
            private var branch: String? = null
            private var existingTitle: String? = null
            private var existingUrl: String? = null
            private var existingState: AnnotaterPullRequests.State? = null

            override fun run(indicator: ProgressIndicator) {
                changes = git.mappingFilesChangedSinceUpstream(project, checkout)
                uncommittedChanges = git.uncommittedMappingFiles(project, checkout)
                commitsAhead = git.commitsAheadOfUpstream(project, checkout)
                branch = git.currentContribution(checkout)?.branch
                existingTitle = branch?.let { git.pullRequestTitle(project, checkout, it) }
                existingUrl = branch?.let { git.pullRequestUrl(project, checkout, it) }
                existingState = existingUrl?.let { lookUpState(it) }
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
                showDialog(
                    project,
                    checkout,
                    git,
                    branch,
                    changes,
                    uncommittedChanges,
                    existingTitle,
                    existingUrl,
                    existingState,
                )
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
        previousUrl: String?,
        previousState: AnnotaterPullRequests.State?,
    ) {
        // A closed pull request can't be updated, so a new one is opened instead
        val previous = previousUrl?.let { AnnotaterPullRequests.PullRequest(it, previousState) }
        val existingUrl = previous?.takeUnless { it.closed }?.url
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

        val dialog = AnnotaterPullRequestDialog(
            project,
            accounts,
            defaultAccount,
            changes,
            uncommittedChanges,
            existingTitle,
            update = existingUrl != null,
            notice = previousUrl?.let { url ->
                when (previousState) {
                    AnnotaterPullRequests.State.MERGED -> MCDevBundle("fabric.annotater.pr.dialog.merged", url)
                    AnnotaterPullRequests.State.CLOSED -> MCDevBundle("fabric.annotater.pr.dialog.closed", url)
                    else -> MCDevBundle("fabric.annotater.pr.dialog.existing", url)
                }
            },
        )
        if (!dialog.showAndGet()) {
            return
        }
        val account = dialog.account ?: return
        if (dialog.checkBuild && !AnnotaterBuildCheck.run(project, checkout)) {
            return
        }
        val token = GHCompatibilityUtil.getOrRequestToken(account, project) ?: return

        val request = Submission(dialog.prTitle, dialog.prDescription, dialog.commitMessage, branch, existingUrl)
        val title = if (existingUrl != null) "fabric.annotater.pr.updating" else "fabric.annotater.pr.creating"
        object : Task.Backgroundable(project, MCDevBundle(title), false) {
            private var url: String? = null

            override fun run(indicator: ProgressIndicator) {
                val url =
                    submit(project, checkout, git, account, token, request, uncommittedChanges.isNotEmpty(), indicator)
                this.url = url
                states[url] = CachedState(AnnotaterPullRequests.State.OPEN, System.currentTimeMillis())
                git.setPullRequest(project, checkout, request.branch, url)
                if (existingUrl == null) {
                    // Label the contribution with its pull request, for switching back to it later
                    runCatching { git.setContributionTitle(project, checkout, request.branch, request.title) }
                }
            }

            override fun onSuccess() {
                val url = url ?: return
                val notificationTitle =
                    if (existingUrl != null) "fabric.annotater.pr.updated" else "fabric.annotater.pr.created"
                Notification(
                    AnnotaterNotifications.GROUP_ID,
                    MCDevBundle(notificationTitle),
                    url,
                    NotificationType.INFORMATION,
                ).addAction(
                    NotificationAction.createSimple(MCDevBundle("fabric.annotater.pr.open")) {
                        BrowserUtil.browse(url)
                    },
                ).addAction(
                    NotificationAction.createSimpleExpiring(MCDevBundle("fabric.annotater.pr.start_new")) {
                        AnnotaterJavadocContributions.switchContribution(project, checkout, null)
                    },
                ).notify(project)
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.pr.failed"), error)
            }
        }.queue()
    }

    /**
     * @property existingUrl the URL of the pull request to update, or null to create one
     */
    private class Submission(
        val title: String,
        val body: String,
        val commitMessage: String,
        val branch: String,
        val existingUrl: String?,
    )

    /**
     * Commits and pushes the changes, and creates the pull request unless it already exists. Returns its URL.
     */
    @Throws(IOException::class, VcsException::class)
    private fun submit(
        project: Project,
        checkout: AnnotaterCheckout,
        git: AnnotaterGitVcs,
        account: GithubAccount,
        token: String,
        request: Submission,
        hasChanges: Boolean,
        indicator: ProgressIndicator,
    ): String {
        val api = GitHubApi(account.server.toApiUrl(), token)
        val upstream = AnnotaterGitVcs.upstreamRepository

        // Otherwise GitHub only rejects the pull request with "base invalid" after committing, forking and pushing
        if (request.existingUrl == null && !api.exists("/repos/$upstream/branches/${checkout.version}")) {
            throw IOException(MCDevBundle("fabric.annotater.pr.no_base_branch", upstream, checkout.version))
        }

        if (hasChanges) {
            indicator.text = MCDevBundle("fabric.annotater.pr.committing")
            git.commitMappings(project, checkout, request.commitMessage)
        }

        // Returns the existing fork if there is one
        indicator.text = MCDevBundle("fabric.annotater.pr.forking")
        val fork = api.post("/repos/$upstream/forks", JsonObject())
        val forkName = fork.get("full_name").asString
        val forkOwner = fork.getAsJsonObject("owner").get("login").asString
        val cloneUrl = fork.get("clone_url").asString
        waitForFork(api, forkName, indicator)

        // Name the fork's owner in the URL, so git credential helpers and the GitHub plugin authenticate the push as
        // the selected account, rather than whichever account they'd use for github.com by default
        val pushUrl = URI(cloneUrl).let { uri ->
            URI(uri.scheme, forkOwner, uri.host, uri.port, uri.path, uri.query, uri.fragment).toString()
        }
        indicator.text = MCDevBundle("fabric.annotater.pr.pushing")
        git.push(project, checkout, pushUrl, request.branch)
        if (request.existingUrl != null) {
            return request.existingUrl
        }

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
     * Looks up the state of the upstream pull request at [url] and caches it, updating the editor banners if it
     * changed. Returns null if it can't be looked up, for example when offline or rate limited, in which case the
     * previously cached state is kept. Pull requests are public, so this doesn't need an account.
     */
    private fun lookUpState(url: String): AnnotaterPullRequests.State? {
        val state = fetchState(url)
        val old = states[url]?.state
        states[url] = CachedState(state ?: old, System.currentTimeMillis())
        if (state != null && state != old) {
            EditorNotifications.updateAll()
        }
        return state
    }

    private fun fetchState(url: String): AnnotaterPullRequests.State? {
        val number = url.substringAfterLast("/pull/", "").toIntOrNull() ?: return null
        val pullRequest = runCatching {
            GitHubApi(GithubServerPath.DEFAULT_SERVER.toApiUrl(), null)
                .get("/repos/${AnnotaterGitVcs.upstreamRepository}/pulls/$number").asJsonObject
        }.getOrNull() ?: return null
        return when {
            pullRequest.get("merged")?.asBoolean == true -> AnnotaterPullRequests.State.MERGED
            pullRequest.get("state")?.asString == "open" -> AnnotaterPullRequests.State.OPEN
            else -> AnnotaterPullRequests.State.CLOSED
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

    /**
     * @param token the account's token, or null to make unauthenticated requests
     */
    private class GitHubApi(private val baseUrl: String, token: String?) {
        private val gson = Gson()
        private val headers = listOfNotNull(
            token?.let { "Authorization" to "Bearer $it" },
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        ).toMap()

        fun post(path: String, body: JsonObject): JsonObject {
            return McdevHttp.post(baseUrl + path, "application/json", headers)
                .throwStatusCodeException(false)
                .connect { request ->
                    request.write(gson.toJson(body))
                    gson.fromJson(readResponse(request), JsonObject::class.java)
                }
        }

        fun get(path: String): JsonElement {
            return McdevHttp.request(baseUrl + path, headers)
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
                ?: return MCDevBundle("fabric.annotater.github.error.http_body", statusCode, body)
            val message = json.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                ?: MCDevBundle("fabric.annotater.github.error.http", statusCode)
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
            return if (details.isEmpty()) {
                message
            } else {
                MCDevBundle("fabric.annotater.github.error.details", message, details.joinToString("; "))
            }
        }
    }

    companion object {
        private const val FORK_POLL_ATTEMPTS = 30
        private const val FORK_POLL_DELAY_MS = 2000L

        /**
         * How long a pull request's state is shown before it's looked up again.
         */
        private const val STATE_CACHE_MS = 5 * 60 * 1000L
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}
