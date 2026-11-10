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

package com.demonwav.mcdev.platform.fabric.annotater

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

/**
 * Git operations on managed annotater checkouts. Only available when the Git plugin is installed.
 */
interface AnnotaterVcs {
    /**
     * Clones annotater if needed, fetches it, and creates or updates the worktree for [version], in the background
     * with a notification when done, or with a modal progress dialog if [modal].
     */
    fun cloneOrUpdate(
        project: Project?,
        version: String,
        modal: Boolean = false,
        onFinished: (Result<AnnotaterCheckout>) -> Unit = {},
    )

    /**
     * Makes sure the managed [checkout] is on a contribution branch rather than the upstream version branch, so that
     * edits can later be pushed and turned into a pull request. Must not be called on the EDT.
     */
    fun prepareForEditing(project: Project?, checkout: AnnotaterCheckout)

    /**
     * The contribution [checkout] has checked out, or null if HEAD is detached or this isn't a git repository. This is
     * cheap enough to call from any thread.
     */
    fun currentContribution(checkout: AnnotaterCheckout): AnnotaterContribution?

    /**
     * The contribution branches in the managed [checkout], newest first. Must not be called on the EDT.
     */
    fun contributions(project: Project?, checkout: AnnotaterCheckout): List<AnnotaterContribution>

    /**
     * The mapping files in [checkout] that have changed since the last commit. Must not be called on the EDT.
     */
    fun uncommittedMappingFiles(project: Project?, checkout: AnnotaterCheckout): List<String>

    /**
     * Commits any changes in the managed [checkout] with [commitMessage], then checks out the contribution [branch],
     * or the latest upstream version branch to start a new contribution if it's null. Must not be called on the EDT.
     */
    fun switchContribution(
        project: Project?,
        checkout: AnnotaterCheckout,
        branch: String?,
        commitMessage: String?,
        indicator: ProgressIndicator,
    )

    companion object {
        val instance: AnnotaterVcs?
            get() = ApplicationManager.getApplication().getService(AnnotaterVcs::class.java)
    }
}

/**
 * A local branch in a managed checkout holding one contribution, which becomes one pull request.
 *
 * @property title the title of the contribution's pull request, or its latest commit message if it doesn't have one
 * yet, or null if nothing has been committed to it
 */
data class AnnotaterContribution(val branch: String, val title: String?) {
    val displayName: String
        get() = title ?: branch

    companion object {
        /**
         * A pull request title or commit message describing changes to the mapping [files].
         */
        fun defaultTitle(files: List<String>): String {
            val classes = files.map { it.substringAfterLast('/').removeSuffix(".mapping") }.distinct()
            return when {
                classes.isEmpty() -> ""
                classes.size <= 3 -> "Document ${classes.joinToString(", ")}"
                else -> "Document ${classes.take(3).joinToString(", ")} and ${classes.size - 3} more"
            }
        }
    }
}

/**
 * Creating pull requests to annotater. Only available when the GitHub plugin is installed.
 */
interface AnnotaterPullRequests {
    fun canCreatePullRequest(checkout: AnnotaterCheckout): Boolean = checkout.managed

    fun createPullRequest(project: Project, checkout: AnnotaterCheckout)

    companion object {
        val instance: AnnotaterPullRequests?
            get() = ApplicationManager.getApplication().getService(AnnotaterPullRequests::class.java)
    }
}
