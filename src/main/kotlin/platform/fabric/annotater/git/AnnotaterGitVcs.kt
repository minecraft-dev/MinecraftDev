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

package com.demonwav.mcdev.platform.fabric.annotater.git

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckout
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterContribution
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterListener
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterNotifications
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterSettings
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterVcs
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.AppExecutorUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

class AnnotaterGitVcs : AnnotaterVcs {
    private val lock = Any()

    /**
     * Pull request titles of contribution branches, by branch name.
     */
    private val titles = ConcurrentHashMap<String, String>()

    /**
     * [AnnotaterContribution.title]s of contribution branches, by branch name, so that the editor banner can show them
     * without running git.
     */
    private val contributionTitles = ConcurrentHashMap<String, String>()

    /**
     * The branches whose [contributionTitles] have been looked up at least once.
     */
    private val titledBranches = ConcurrentHashMap.newKeySet<String>()

    /**
     * The [PushStatus] of each managed checkout by root, so that the editor banner can show it without running git.
     */
    private val pushStatuses = ConcurrentHashMap<Path, PushStatus>()

    /**
     * The checkouts whose [pushStatuses] have been requested, to refresh them when they change.
     */
    private val pushStatusCheckouts = ConcurrentHashMap<Path, AnnotaterCheckout>()

    /**
     * The checkouts whose [pushStatuses] are waiting to be refreshed.
     */
    private val pendingPushStatuses = ConcurrentHashMap.newKeySet<Path>()

    private val pushStatusExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("Annotater push status", 1)

    init {
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AnnotaterListener.TOPIC,
            object : AnnotaterListener {
                override fun checkoutsChanged() {
                    pushStatusCheckouts.values.forEach(::refreshPushStatus)
                }

                override fun javadocsChanged(checkoutRoot: Path, outerClass: String) {
                    pushStatusCheckouts[checkoutRoot]?.let(::refreshPushStatus)
                }
            },
        )
    }

    override fun cloneOrUpdate(
        project: Project?,
        version: String,
        modal: Boolean,
        onFinished: (Result<AnnotaterCheckout>) -> Unit,
    ) {
        val title = MCDevBundle("fabric.annotater.cloning", version)
        var checkout: AnnotaterCheckout? = null

        fun doRun(indicator: ProgressIndicator) {
            checkout = cloneOrUpdateNow(project, version, indicator)
        }

        fun doSuccess() {
            val checkout = checkout ?: return
            ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
            if (!modal) {
                AnnotaterNotifications.notifyInfo(
                    project,
                    MCDevBundle("fabric.annotater.cloned", version),
                    MCDevBundle("fabric.annotater.cloned.content", checkout.root),
                )
            }
            onFinished(Result.success(checkout))
        }

        fun doFail(error: Throwable) {
            if (!modal) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.clone_failed"), error)
            }
            onFinished(Result.failure(error))
        }

        val task = if (modal) {
            object : Task.Modal(project, title, true) {
                override fun run(indicator: ProgressIndicator) = doRun(indicator)
                override fun onSuccess() = doSuccess()
                override fun onThrowable(error: Throwable) = doFail(error)
            }
        } else {
            object : Task.Backgroundable(project, title, true) {
                override fun run(indicator: ProgressIndicator) = doRun(indicator)
                override fun onSuccess() = doSuccess()
                override fun onThrowable(error: Throwable) = doFail(error)
            }
        }
        task.queue()
    }

    @Throws(VcsException::class)
    private fun cloneOrUpdateNow(project: Project?, version: String, indicator: ProgressIndicator): AnnotaterCheckout {
        val gitProject = project ?: ProjectManager.getInstance().defaultProject
        synchronized(lock) {
            val root = AnnotaterSettings.instance.effectiveManagedRoot
            val repo = root.resolve(REPO_DIR)
            if (!repo.resolve(".git").exists()) {
                indicator.text = MCDevBundle("fabric.annotater.cloning.clone")
                Files.createDirectories(root)
                val result = Git.getInstance().clone(gitProject, root, upstreamUrl, REPO_DIR)
                if (!result.success()) {
                    throw VcsException(result.errorOutputAsJoinedString)
                }
            } else {
                indicator.text = MCDevBundle("fabric.annotater.cloning.fetch")
                run(gitProject, repo, GitCommand.FETCH, "--prune", UPSTREAM_REMOTE, url = upstreamUrl)
            }

            val remoteBranch = "$UPSTREAM_REMOTE/$version"
            val hasBranch = runCatching {
                run(gitProject, repo, GitCommand.REV_PARSE, "--verify", "--quiet", "refs/remotes/$remoteBranch")
            }.isSuccess
            if (!hasBranch) {
                throw VcsException(MCDevBundle("fabric.annotater.no_branch", version))
            }

            val worktree = AnnotaterCheckoutService.instance.managedWorktree(version)
            if (!worktree.exists()) {
                indicator.text = MCDevBundle("fabric.annotater.cloning.worktree")
                run(gitProject, repo, GitCommand.WORKTREE, "prune")
                run(gitProject, repo, GitCommand.WORKTREE, "add", "--detach", worktree.toString(), remoteBranch)
            } else if (currentBranch(gitProject, worktree) == null && isClean(gitProject, worktree)) {
                // Not contributing yet, so follow upstream
                run(gitProject, worktree, GitCommand.CHECKOUT, "--detach", remoteBranch)
            }

            LocalFileSystem.getInstance().refreshNioFiles(listOf(worktree), true, true, null)
            ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
            return AnnotaterCheckoutService.instance.checkoutForVersion(version)
                ?: throw VcsException(MCDevBundle("fabric.annotater.invalid_after_clone", worktree))
        }
    }

    override fun prepareForEditing(project: Project?, checkout: AnnotaterCheckout) {
        if (!checkout.managed) {
            return
        }
        val gitProject = project ?: ProjectManager.getInstance().defaultProject
        synchronized(lock) {
            if (currentBranch(gitProject, checkout.root) != null) {
                refreshCurrentTitle(gitProject, checkout)
                return
            }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            run(gitProject, checkout.root, GitCommand.CHECKOUT, "-b", "${branchPrefix(checkout)}$timestamp")
        }
        ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
    }

    override fun currentContribution(checkout: AnnotaterCheckout): AnnotaterContribution? {
        val branch = readHeadBranch(checkout.root) ?: return null
        if (checkout.managed && titledBranches.add(branch)) {
            // Otherwise the banner shows the branch name until the contributions are listed or a Javadoc is edited
            ApplicationManager.getApplication().executeOnPooledThread {
                synchronized(lock) {
                    runCatching { refreshCurrentTitle(ProjectManager.getInstance().defaultProject, checkout) }
                }
            }
        }
        return AnnotaterContribution(branch, contributionTitles[branch])
    }

    /**
     * Reads the checked out branch straight from the worktree's `HEAD` file, to avoid running git.
     */
    private fun readHeadBranch(root: Path): String? {
        return try {
            val dotGit = root.resolve(".git")
            val gitDir = if (dotGit.isDirectory()) {
                dotGit
            } else {
                val gitDirPath = dotGit.readText().trim().removePrefix("gitdir:").trim()
                root.resolve(gitDirPath)
            }
            // A detached HEAD contains a commit hash instead
            val head = gitDir.resolve("HEAD").readText().trim()
            head.takeIf { it.startsWith(HEAD_REF_PREFIX) }?.removePrefix(HEAD_REF_PREFIX)
        } catch (e: IOException) {
            null
        }
    }

    override fun contributions(project: Project?, checkout: AnnotaterCheckout): List<AnnotaterContribution> {
        val gitProject = project ?: ProjectManager.getInstance().defaultProject
        loadTitles(gitProject, checkout)
        val branches = run(
            gitProject,
            checkout.root,
            GitCommand.FOR_EACH_REF,
            "--sort=-refname",
            "--format=%(refname:short)",
            "refs/heads/${branchPrefix(checkout)}",
        ).map { it.trim() }.filter { it.isNotEmpty() }
        return branches.map { AnnotaterContribution(it, contributionTitle(gitProject, checkout, it)) }
    }

    /**
     * The pull request title of the contribution [branch], or its latest commit message if it doesn't have one yet.
     */
    private fun contributionTitle(project: Project, checkout: AnnotaterCheckout, branch: String): String? {
        val title = titles[branch] ?: run(
            project,
            checkout.root,
            GitCommand.LOG,
            "-1",
            "--format=%s",
            "$UPSTREAM_REMOTE/${checkout.version}..$branch",
            "--",
        ).firstOrNull()?.takeIf { it.isNotBlank() }
        if (title != null) {
            contributionTitles[branch] = title
        } else {
            contributionTitles.remove(branch)
        }
        return title
    }

    /**
     * Updates the title of the checked out contribution shown in editor banners.
     */
    private fun refreshCurrentTitle(project: Project, checkout: AnnotaterCheckout) {
        val branch = currentBranch(project, checkout.root) ?: return
        loadTitles(project, checkout)
        val old = contributionTitles[branch]
        if (runCatching { contributionTitle(project, checkout, branch) }.getOrNull() != old) {
            EditorNotifications.updateAll()
        }
    }

    /**
     * The title of the pull request already opened for the contribution [branch], if any.
     */
    fun pullRequestTitle(project: Project, checkout: AnnotaterCheckout, branch: String): String? {
        loadTitles(project, checkout)
        return titles[branch]
    }

    /**
     * Loads the pull request titles stored as branch descriptions by [setContributionTitle].
     */
    private fun loadTitles(project: Project, checkout: AnnotaterCheckout) {
        val output = runCatching {
            run(project, checkout.root, GitCommand.CONFIG, "--get-regexp", "^branch\\..*\\.description$")
        }.getOrDefault(emptyList())
        for (line in output) {
            val key = line.substringBefore(' ')
            val value = line.substringAfter(' ', "").trim()
            if (value.isNotEmpty()) {
                titles[key.removePrefix("branch.").removeSuffix(".description")] = value
            }
        }
    }

    /**
     * Remembers the title of the pull request for the contribution [branch], as its git branch description.
     */
    fun setContributionTitle(project: Project, checkout: AnnotaterCheckout, branch: String, title: String) {
        run(project, checkout.root, GitCommand.CONFIG, "branch.$branch.description", title)
        titles[branch] = title
        contributionTitles[branch] = title
        EditorNotifications.updateAll()
    }

    /**
     * Whether the contribution checked out in a managed checkout has a pull request, and whether it has changes that
     * haven't been pushed to it, or to a new pull request if it doesn't have one.
     */
    data class PushStatus(val pullRequestUrl: String?, val hasUnpushedChanges: Boolean)

    /**
     * The [PushStatus] of the managed [checkout], or null if it isn't known yet, in which case it's looked up in the
     * background and the editor banners are updated. This is cheap enough to call from any thread.
     */
    fun pushStatus(checkout: AnnotaterCheckout): PushStatus? {
        if (!checkout.managed) {
            return null
        }
        if (pushStatusCheckouts.put(checkout.root, checkout) == null) {
            refreshPushStatus(checkout)
        }
        return pushStatuses[checkout.root]
    }

    private fun refreshPushStatus(checkout: AnnotaterCheckout) {
        val root = checkout.root
        if (!pendingPushStatuses.add(root)) {
            return
        }
        pushStatusExecutor.execute {
            pendingPushStatuses.remove(root)
            val status = runCatching { computePushStatus(ProjectManager.getInstance().defaultProject, checkout) }
                .getOrNull()
            val old = if (status == null) pushStatuses.remove(root) else pushStatuses.put(root, status)
            if (old != status) {
                EditorNotifications.updateAll()
            }
        }
    }

    private fun computePushStatus(project: Project, checkout: AnnotaterCheckout): PushStatus {
        val branch = currentBranch(project, checkout.root) ?: return PushStatus(null, false)
        val pullRequestUrl = getBranchConfig(project, checkout, branch, PULL_REQUEST_KEY)
        val pushedCommit = getBranchConfig(project, checkout, branch, PUSHED_COMMIT_KEY)
        val hasUnpushedChanges = when {
            uncommittedMappingFiles(project, checkout).isNotEmpty() -> true
            pullRequestUrl != null && pushedCommit != null -> headCommit(project, checkout) != pushedCommit
            else -> commitsAheadOfUpstream(project, checkout) > 0
        }
        return PushStatus(pullRequestUrl, hasUnpushedChanges)
    }

    /**
     * The URL of the pull request opened for the contribution [branch], if any.
     */
    fun pullRequestUrl(project: Project, checkout: AnnotaterCheckout, branch: String): String? =
        getBranchConfig(project, checkout, branch, PULL_REQUEST_KEY)

    /**
     * Remembers that the contribution [branch] has been pushed to the pull request at [url], so that the editor banner
     * offers to update it when there are new changes.
     */
    fun setPullRequest(project: Project, checkout: AnnotaterCheckout, branch: String, url: String) {
        run(project, checkout.root, GitCommand.CONFIG, "branch.$branch.$PULL_REQUEST_KEY", url)
        run(
            project,
            checkout.root,
            GitCommand.CONFIG,
            "branch.$branch.$PUSHED_COMMIT_KEY",
            headCommit(project, checkout),
        )
        refreshPushStatus(checkout)
    }

    private fun getBranchConfig(project: Project, checkout: AnnotaterCheckout, branch: String, key: String): String? {
        // git config exits with 1 when the key isn't set
        return runCatching { run(project, checkout.root, GitCommand.CONFIG, "--get", "branch.$branch.$key") }
            .getOrNull()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun headCommit(project: Project, checkout: AnnotaterCheckout): String =
        run(project, checkout.root, GitCommand.REV_PARSE, "HEAD").first().trim()

    override fun uncommittedMappingFiles(project: Project?, checkout: AnnotaterCheckout): List<String> {
        val output = run(
            project ?: ProjectManager.getInstance().defaultProject,
            checkout.root,
            GitCommand.STATUS,
            "--porcelain",
            "--untracked-files=all",
            "--",
            checkout.config.mappings,
        )
        return output.mapNotNull { line -> line.takeIf { it.length > 3 }?.substring(3) }
    }

    /**
     * The mapping files changed on the current branch since it diverged from the upstream version branch, whether
     * committed or not.
     */
    fun mappingFilesChangedSinceUpstream(project: Project, checkout: AnnotaterCheckout): List<String> {
        val base = run(project, checkout.root, GitCommand.MERGE_BASE, "$UPSTREAM_REMOTE/${checkout.version}", "HEAD")
            .first().trim()
        val committed =
            run(project, checkout.root, GitCommand.DIFF, "--name-only", base, "--", checkout.config.mappings)
        return (committed + uncommittedMappingFiles(project, checkout)).map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
    }

    fun commitMappings(project: Project, checkout: AnnotaterCheckout, message: String) {
        synchronized(lock) {
            run(project, checkout.root, GitCommand.ADD, "--all", "--", checkout.config.mappings)
            run(project, checkout.root, GitCommand.COMMIT, "-m", message)
        }
    }

    /**
     * The number of commits on the current branch that aren't in the upstream version branch.
     */
    fun commitsAheadOfUpstream(project: Project, checkout: AnnotaterCheckout): Int {
        val output = run(
            project,
            checkout.root,
            GitCommand.REV_LIST,
            "--count",
            "$UPSTREAM_REMOTE/${checkout.version}..HEAD",
        )
        return output.firstOrNull()?.trim()?.toIntOrNull() ?: 0
    }

    fun push(project: Project, checkout: AnnotaterCheckout, url: String, branch: String) {
        run(project, checkout.root, GitCommand.PUSH, url, "HEAD:refs/heads/$branch", url = url)
    }

    override fun switchContribution(
        project: Project?,
        checkout: AnnotaterCheckout,
        branch: String?,
        commitMessage: String?,
        indicator: ProgressIndicator,
    ) {
        val gitProject = project ?: ProjectManager.getInstance().defaultProject
        synchronized(lock) {
            if (commitMessage != null && uncommittedMappingFiles(gitProject, checkout).isNotEmpty()) {
                if (currentBranch(gitProject, checkout.root) == null) {
                    throw VcsException(MCDevBundle("fabric.annotater.uncommitted_changes"))
                }
                indicator.text = MCDevBundle("fabric.annotater.pr.committing")
                commitMappings(gitProject, checkout, commitMessage)
            }
            if (!isClean(gitProject, checkout.root)) {
                throw VcsException(MCDevBundle("fabric.annotater.uncommitted_changes"))
            }
            if (branch == null) {
                indicator.text = MCDevBundle("fabric.annotater.cloning.fetch")
                val repo = AnnotaterSettings.instance.effectiveManagedRoot.resolve(REPO_DIR)
                run(gitProject, repo, GitCommand.FETCH, "--prune", UPSTREAM_REMOTE, url = upstreamUrl)
                run(gitProject, checkout.root, GitCommand.CHECKOUT, "--detach", "$UPSTREAM_REMOTE/${checkout.version}")
            } else {
                run(gitProject, checkout.root, GitCommand.CHECKOUT, branch)
            }
            refreshCurrentTitle(gitProject, checkout)
        }
        LocalFileSystem.getInstance().refreshNioFiles(listOf(checkout.root), true, true, null)
        ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
    }

    override fun deleteContribution(
        project: Project?,
        checkout: AnnotaterCheckout,
        branch: String,
        indicator: ProgressIndicator,
    ) {
        val gitProject = project ?: ProjectManager.getInstance().defaultProject
        synchronized(lock) {
            if (currentBranch(gitProject, checkout.root) == branch) {
                // Discard the contribution's uncommitted changes, including new mapping files
                val untracked = run(
                    gitProject,
                    checkout.root,
                    GitCommand.STATUS,
                    "--porcelain",
                    "--untracked-files=all",
                    "--",
                    checkout.config.mappings,
                ).filter { it.startsWith("?? ") }.map { checkout.root.resolve(it.substring(3)) }
                run(
                    gitProject,
                    checkout.root,
                    GitCommand.CHECKOUT,
                    "--force",
                    "--detach",
                    "$UPSTREAM_REMOTE/${checkout.version}",
                )
                untracked.forEach { Files.deleteIfExists(it) }
            }
            // Also deletes the branch's configuration, such as its pull request title
            run(gitProject, checkout.root, GitCommand.BRANCH, "-D", branch)
            titles.remove(branch)
            contributionTitles.remove(branch)
            titledBranches.remove(branch)
        }
        LocalFileSystem.getInstance().refreshNioFiles(listOf(checkout.root), true, true, null)
        ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
    }

    private fun branchPrefix(checkout: AnnotaterCheckout) = "mcdev/${checkout.version}/"

    private fun currentBranch(project: Project, dir: Path): String? {
        val output = runCatching { run(project, dir, GitCommand.REV_PARSE, "--abbrev-ref", "HEAD") }.getOrNull()
        return output?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() && it != "HEAD" }
    }

    private fun isClean(project: Project, dir: Path): Boolean {
        return run(project, dir, GitCommand.STATUS, "--porcelain").all { it.isBlank() }
    }

    @Throws(VcsException::class)
    private fun run(
        project: Project,
        dir: Path,
        command: GitCommand,
        vararg parameters: String,
        url: String? = null,
    ): List<String> {
        val handler = GitLineHandler(project, dir, command)
        handler.addParameters(*parameters)
        if (url != null) {
            handler.setUrl(url)
        }
        handler.setSilent(true)
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw VcsException(
                result.errorOutputAsJoinedString.ifBlank { "git ${command.name()} failed with ${result.exitCode}" },
            )
        }
        return result.output
    }

    companion object {
        private const val REPO_DIR = ".repo"
        private const val HEAD_REF_PREFIX = "ref: refs/heads/"
        private const val UPSTREAM_REMOTE = "origin"

        /**
         * The git config keys of contribution branches, under `branch.<name>`, storing the URL of their pull request
         * and the commit last pushed to it.
         */
        private const val PULL_REQUEST_KEY = "mcdevPullRequest"
        private const val PUSHED_COMMIT_KEY = "mcdevPushedCommit"

        /**
         * The GitHub repository annotater is cloned from and pull requests are opened against, as `owner/name`. Can
         * be overridden to test against a fork without opening real pull requests.
         */
        val upstreamRepository: String = System.getProperty("mcdev.annotater.upstream", "FabricMC/annotater")
        private val upstreamUrl = "https://github.com/$upstreamRepository.git"

        val instance: AnnotaterGitVcs?
            get() = AnnotaterVcs.instance as? AnnotaterGitVcs
    }
}
