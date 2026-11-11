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
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterContribution
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import java.awt.Dimension
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount

class AnnotaterPullRequestDialog(
    project: Project,
    accounts: List<GithubAccount>,
    defaultAccount: GithubAccount?,
    private val changes: List<String>,
    private val uncommittedChanges: List<String>,
    existingTitle: String?,
    /**
     * Whether the changes are pushed to an existing pull request, so there's no title or description to enter.
     */
    private val update: Boolean,
) : DialogWrapper(project) {
    private val accountModel = CollectionComboBoxModel(accounts, defaultAccount ?: accounts.first())

    var prTitle = existingTitle ?: AnnotaterContribution.defaultTitle(changes)
    var prDescription = ""
    var commitMessage = AnnotaterContribution.defaultTitle(uncommittedChanges)
    var checkBuild = true

    val account: GithubAccount?
        get() = accountModel.selected

    init {
        if (update) {
            title = MCDevBundle("fabric.annotater.update_pull_request")
            setOKButtonText(MCDevBundle("fabric.annotater.pr.dialog.update"))
        } else {
            title = MCDevBundle("fabric.annotater.create_pull_request")
            setOKButtonText(MCDevBundle("fabric.annotater.pr.dialog.ok"))
        }
        init()
    }

    override fun createCenterPanel() = panel {
        row(MCDevBundle("fabric.annotater.pr.dialog.account")) {
            comboBox(accountModel, SimpleListCellRenderer.create("") { it.name })
                .align(AlignX.FILL)
        }
        if (!update) {
            row(MCDevBundle("fabric.annotater.pr.dialog.title")) {
                textField().bindText(::prTitle).align(AlignX.FILL).focused()
            }
            row(MCDevBundle("fabric.annotater.pr.dialog.description")) {
                textArea().bindText(::prDescription).rows(6).align(AlignX.FILL)
            }
        }
        if (uncommittedChanges.isNotEmpty()) {
            row(MCDevBundle("fabric.annotater.pr.dialog.commit_message")) {
                val field = textField().bindText(::commitMessage).align(AlignX.FILL)
                if (update) {
                    field.focused()
                }
            }
        }
        row {
            checkBox(MCDevBundle("fabric.annotater.pr.dialog.check_build"))
                .bindSelected(::checkBuild)
                .comment(MCDevBundle("fabric.annotater.pr.dialog.check_build.comment"))
        }
        if (changes.isNotEmpty()) {
            row {
                comment(
                    MCDevBundle(
                        "fabric.annotater.pr.dialog.changes",
                        changes.joinToString("<br>") { it.substringAfterLast('/') },
                    ),
                )
            }
        }
    }.apply {
        preferredSize = Dimension(560, preferredSize.height)
    }
}
