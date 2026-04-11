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

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.CreatorCredentials
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.refreshSync
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import com.github.kittinunf.result.getOrNull
import com.github.kittinunf.result.onError
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.equalsTo
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.observable.util.trim
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.createDirectories
import java.awt.Component
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

open class RemoteTemplateProvider : TemplateProvider {

    private var updatedTemplates = mutableSetOf<String>()

    override val label: String = MCDevBundle("template.provider.remote.label")

    override val hasConfig: Boolean = true

    override suspend fun init(indicator: ProgressIndicator, repos: List<MinecraftSettings.TemplateRepo>) {
        for (repo in repos) {
            ProgressManager.checkCanceled()
            val remote = RemoteTemplateRepo.deserialize(repo.data)
                ?: continue
            if (!remote.autoUpdate || remote.url in updatedTemplates) {
                continue
            }

            if (doUpdateRepo(indicator, repo.name, remote.url, remote.authType, remote.authCredentials)) {
                updatedTemplates.add(remote.url)
            }
        }
    }

    protected suspend fun doUpdateRepo(
        indicator: ProgressIndicator,
        repoName: String,
        originalRepoUrl: String,
        authType: RemoteAuthType,
        credentials: String
    ): Boolean {
        indicator.text2 = "Updating remote repository $repoName"

        val repoUrl = replaceVariables(originalRepoUrl, TemplateDescriptor.LATEST_FORMAT_VERSION)

        val manager = FuelManager()
        manager.proxy = selectProxy(repoUrl)
        var request = manager.get(repoUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", PluginUtil.useragent)
            .timeout(10000)

        if (credentials.isNotBlank()) {
            request = CreatorCredentials.configureAuthorization(request, originalRepoUrl, authType, credentials)
        }

        val result = request.awaitByteArrayResult()

        val data = result.onError {
            thisLogger().warn("Could not fetch remote templates repository update at $repoUrl", it)
        }.getOrNull() ?: return false

        try {
            val zipPath = RemoteTemplateRepo.getDestinationZip(repoName)
            zipPath.parent.createDirectories()
            zipPath.writeBytes(data)

            thisLogger().info("Remote templates repository update applied successfully")
            return true
        } catch (t: Throwable) {
            if (t is ControlFlowException) {
                throw t
            }
            thisLogger().error("Failed to apply remote templates repository update of $repoName", t)
        }
        return false
    }

    override suspend fun loadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo
    ): Collection<LoadedTemplate> {
        val remoteRepo = RemoteTemplateRepo.deserialize(repo.data)
            ?: return emptyList()
        return doLoadTemplates(context, repo, remoteRepo.innerPath)
    }

    protected suspend fun doLoadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo,
        rawInnerPath: String,
        version: Int? = null
    ): List<LoadedTemplate> {
        val remoteRootPath = RemoteTemplateRepo.getDestinationZip(repo.name)
        if (!remoteRootPath.exists()) {
            return emptyList()
        }

        val archiveRoot = remoteRootPath.absolutePathString() + JarFileSystem.JAR_SEPARATOR

        val fs = JarFileSystem.getInstance()
        val rootFile = fs.refreshAndFindFileByPath(archiveRoot)
            ?: return emptyList()
        val modalityState = context.modalityState
        rootFile.refreshSync(modalityState)

        // Default to the latest version for unspecified version variable substitution
        val innerPath = replaceVariables(rawInnerPath, version ?: TemplateDescriptor.LATEST_FORMAT_VERSION)
        val repoRoot = if (innerPath.isNotBlank()) {
            rootFile.findFileByRelativePath(innerPath)
        } else {
            rootFile
        }

        if (repoRoot == null) {
            return emptyList()
        }

        return TemplateProvider.findTemplates(modalityState, repoRoot)
    }

    private fun replaceVariables(originalRepoUrl: String, version: Int): String =
        originalRepoUrl.replace($$"$version", version.toString())

    override fun setupConfigUi(
        data: String,
        dataSetter: (String) -> Unit
    ): JComponent? {
        val propertyGraph = PropertyGraph("RemoteTemplateProvider config")
        val defaultRepo = RemoteTemplateRepo.deserialize(data)
        val urlProperty = propertyGraph.property(defaultRepo?.url ?: "").trim()
        val autoUpdateProperty = propertyGraph.property(defaultRepo?.autoUpdate != false)
        val innerPathProperty = propertyGraph.property(defaultRepo?.innerPath ?: "").trim()

        val authTypeProperty = propertyGraph.property(defaultRepo?.authType ?: RemoteAuthType.NONE)

        val initialBasicAuthCredentials =
            defaultRepo?.authCredentials.takeIf { authTypeProperty.get() == RemoteAuthType.BASIC }
        val basicAuthUsernameProperty = propertyGraph.property(initialBasicAuthCredentials ?: "")
        val basicAuthPasswordProperty = propertyGraph.property(
            CreatorCredentials.getCredentials(urlProperty.get(), basicAuthUsernameProperty.get())
                ?.getPasswordAsString() ?: ""
        )

        val bearerTokenProperty = propertyGraph.property(
            CreatorCredentials.getCredentials(urlProperty.get(), null)?.getPasswordAsString() ?: ""
        ).trim()

        val initialGitAuthCredentials =
            defaultRepo?.authCredentials.takeIf { authTypeProperty.get() == RemoteAuthType.GIT_HTTP }
        val gitAuthProviderProperty = propertyGraph.lazyProperty {
            initialGitAuthCredentials?.substringBefore(':')
                ?: CreatorCredentials.getGitHttpAuthProviders().first().providerId
        }
        val gitAuthAccountProperty = propertyGraph.lazyProperty {
            initialGitAuthCredentials?.let(CreatorCredentials::findGitHttpAuthAccount)
        }
        val gitAuthAccountsModel = MutableCollectionComboBoxModel(
            CreatorCredentials.getGitHttpAuthAccounts(gitAuthProviderProperty.get())
        )
        if (gitAuthAccountProperty.get() == null) {
            gitAuthAccountProperty.set(gitAuthAccountsModel.items.firstOrNull())
        }
        gitAuthProviderProperty.afterChange { providerId ->
            gitAuthAccountsModel.update(CreatorCredentials.getGitHttpAuthAccounts(providerId))
        }

        val initialCustomHeader =
            defaultRepo?.authCredentials.takeIf { authTypeProperty.get() == RemoteAuthType.HEADER }
        val headerNameProperty = propertyGraph.property(initialCustomHeader?.substringBefore(':') ?: "")
        val headerValueProperty = propertyGraph.property(
            CreatorCredentials.getCredentials(urlProperty.get(), headerNameProperty.get())?.getPasswordAsString() ?: ""
        )

        val credentialsProperty = authTypeProperty.transform { authType ->
            when (authType) {
                RemoteAuthType.NONE -> ""
                RemoteAuthType.BASIC -> basicAuthUsernameProperty.get()
                RemoteAuthType.BEARER -> ""
                RemoteAuthType.GIT_HTTP -> gitAuthProviderProperty.get() + ':' + gitAuthAccountProperty.get()?.id
                RemoteAuthType.HEADER -> headerNameProperty.get()
            }
        }

        return panel {
            row(MCDevBundle("creator.ui.custom.remote.url.label")) {
                textField()
                    .comment(MCDevBundle("creator.ui.custom.remote.url.comment"))
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(urlProperty)
                    .textValidation(BuiltinValidations.nonBlank)
            }

            row(MCDevBundle("creator.ui.custom.remote.inner_path.label")) {
                textField()
                    .comment(MCDevBundle("creator.ui.custom.remote.inner_path.comment"))
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(innerPathProperty)
            }

            row(MCDevBundle("creator.ui.custom.remote.auth_type.label")) {
                comboBox(
                    EnumComboBoxModel(RemoteAuthType::class.java),
                    object : JLabel(), ListCellRenderer<RemoteAuthType?> {
                        override fun getListCellRendererComponent(
                            list: JList<out RemoteAuthType?>?,
                            value: RemoteAuthType?,
                            index: Int,
                            isSelected: Boolean,
                            cellHasFocus: Boolean
                        ): Component {
                            text = value?.displayname?.let(MCDevBundle::invoke) ?: value?.name?.capitalize().toString()
                            return this
                        }
                    }).bindItem(authTypeProperty)
            }

            row(MCDevBundle("creator.ui.custom.remote.basic_auth.username.label")) {
                textField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(basicAuthUsernameProperty)
                    .addValidationRule("Must not be blank") { field ->
                        authTypeProperty.get() == RemoteAuthType.BASIC && field.text.isBlank()
                    }
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.BASIC))

            row(MCDevBundle("creator.ui.custom.remote.basic_auth.password.label")) {
                passwordField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(basicAuthPasswordProperty)
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.BASIC))

            row(MCDevBundle("creator.ui.custom.remote.bearer_token.label")) {
                passwordField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(bearerTokenProperty)
                    .addValidationRule("Must not be blank") { field ->
                        authTypeProperty.get() == RemoteAuthType.BEARER && field.password.all { it.isWhitespace() }
                    }
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.BEARER))

            row(MCDevBundle("creator.ui.custom.remote.git_http_provider.label")) {
                comboBox(CollectionComboBoxModel(CreatorCredentials.getGitHttpAuthProviders().map { it.providerId }))
                    .align(AlignX.FILL)
                    .bindItem(gitAuthProviderProperty)
                    .validationOnApply { box ->
                        if (authTypeProperty.get() == RemoteAuthType.GIT_HTTP && box.item == null) {
                            error("A provider must be selected")
                        } else null
                    }
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.GIT_HTTP))

            row(MCDevBundle("creator.ui.custom.remote.git_http_account.label")) {
                comboBox(gitAuthAccountsModel)
                    .align(AlignX.FILL)
                    .bindItem(gitAuthAccountProperty)
                    .validationOnApply { box ->
                        if (authTypeProperty.get() == RemoteAuthType.GIT_HTTP && box.item == null) {
                            error("An account must be selected")
                        } else null
                    }
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.GIT_HTTP))

            row(MCDevBundle("creator.ui.custom.remote.custom_header.name.label")) {
                textField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(headerNameProperty)
                    .addValidationRule("Must not be blank") { field ->
                        authTypeProperty.get() == RemoteAuthType.HEADER && field.text.all { it.isWhitespace() }
                    }
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.HEADER))

            row(MCDevBundle("creator.ui.custom.remote.custom_header.value.label")) {
                passwordField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(headerValueProperty)
            }.visibleIf(authTypeProperty.equalsTo(RemoteAuthType.HEADER))

            row {
                checkBox(MCDevBundle("creator.ui.custom.remote.auto_update.label"))
                    .bindSelected(autoUpdateProperty)
            }

            onApply {
                val url = urlProperty.get()
                when (authTypeProperty.get()) {
                    RemoteAuthType.NONE -> Unit
                    RemoteAuthType.BASIC -> CreatorCredentials.persistCredentials(
                        url,
                        basicAuthUsernameProperty.get(),
                        basicAuthPasswordProperty.get()
                    )

                    RemoteAuthType.BEARER -> CreatorCredentials.persistCredentials(url, null, bearerTokenProperty.get())
                    RemoteAuthType.GIT_HTTP -> Unit
                    RemoteAuthType.HEADER -> CreatorCredentials.persistCredentials(
                        url,
                        headerNameProperty.get(),
                        headerValueProperty.get()
                    )
                }

                val repo = RemoteTemplateRepo(
                    url,
                    autoUpdateProperty.get(),
                    innerPathProperty.get(),
                    authTypeProperty.get(),
                    credentialsProperty.get()
                )
                dataSetter(repo.serialize())
            }
        }
    }

    enum class RemoteAuthType(val displayname: @NlsContexts.Label String) {
        NONE("minecraft.settings.creator.repos.column.provider.none"),
        BASIC("minecraft.settings.creator.repos.column.provider.basic"),
        BEARER("minecraft.settings.creator.repos.column.provider.bearer"),
        GIT_HTTP("minecraft.settings.creator.repos.column.provider.git_http"),
        HEADER("minecraft.settings.creator.repos.column.provider.header")
    }

    data class RemoteTemplateRepo(
        val url: String,
        val autoUpdate: Boolean,
        val innerPath: String,
        val authType: RemoteAuthType,
        val authCredentials: String,
    ) {

        fun serialize(): String = "$url\n$autoUpdate\n$innerPath\n${authType.name}\n$authCredentials"

        companion object {

            val templatesBaseDir: Path
                get() = PathManager.getSystemDir().resolve("mcdev-templates")

            fun getDestinationZip(repoName: String): Path {
                return templatesBaseDir.resolve("$repoName.zip")
            }

            fun deserialize(data: String): RemoteTemplateRepo? {
                if (data.isBlank()) {
                    return null
                }

                val lines = data.lines()
                return RemoteTemplateRepo(
                    lines[0],
                    lines.getOrNull(1).toBoolean(),
                    lines.getOrNull(2) ?: "",
                    runCatching { RemoteAuthType.valueOf(lines.getOrNull(3) ?: "") }
                        .getOrDefault(RemoteAuthType.NONE),
                    lines.getOrNull(4) ?: "",
                )
            }
        }
    }
}
