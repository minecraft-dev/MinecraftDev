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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.creator.custom.providers.RemoteTemplateProvider.RemoteAuthType
import com.intellij.collaboration.auth.ServerAccount
import com.intellij.collaboration.auth.findAccountOrNull
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import git4idea.remote.GitHttpAuthDataProvider
import git4idea.remote.hosting.http.SilentHostedGitHttpAuthDataProvider
import java.nio.file.Path
import java.util.Base64
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathNodes
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object CreatorCredentials {

    private val xmlDocumentBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
    private val xPath = XPathFactory.newDefaultInstance().newXPath()

    private fun makeServiceName(url: String): String = generateServiceName("MinecraftDev Creator", url)

    fun persistCredentials(
        url: String,
        username: String?,
        password: String,
    ) {
        val serviceName = makeServiceName(url)
        val credAttrs = CredentialAttributes(serviceName, username)
        PasswordSafe.instance.setPassword(credAttrs, password)
    }

    fun getCredentials(url: String, username: String?): Credentials? {
        val serviceName = makeServiceName(url)
        val credAttrs = CredentialAttributes(serviceName, username)
        return PasswordSafe.instance[credAttrs]
    }

    fun basicAuthHeader(username: String, password: String): Map<String, String> {
        val token = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
        return mapOf("Authorization" to "Basic $token")
    }

    /**
     * Returns the headers that authenticate a request to [url] with the given [authType], or an empty map if no
     * credentials are stored for it.
     */
    suspend fun authorizationHeaders(
        url: String,
        authType: RemoteAuthType,
        credentials: String
    ): Map<String, String> {
        when (authType) {
            RemoteAuthType.NONE -> return emptyMap()

            RemoteAuthType.BASIC -> {
                val creds = getCredentials(url, credentials)
                val username = creds?.userName
                val password = creds?.getPasswordAsString()
                if (username != null && password != null) {
                    return basicAuthHeader(username, password)
                }
            }

            RemoteAuthType.BEARER -> {
                val creds = getCredentials(url, null)
                val password = creds?.getPasswordAsString()
                if (password != null) {
                    return mapOf("Authorization" to "Bearer $password")
                }
            }

            RemoteAuthType.GIT_HTTP -> {
                val creds = findGitHttpAuthBearerToken(credentials)
                if (creds != null) {
                    return basicAuthHeader(creds.first, creds.second)
                }
            }

            RemoteAuthType.HEADER -> {
                val creds = getCredentials(url, credentials)
                val username = creds?.userName
                val password = creds?.getPasswordAsString()
                if (username != null && password != null) {
                    return mapOf(username to password)
                }
            }
        }

        return emptyMap()
    }

    fun getGitHttpAuthProviders(): List<SilentHostedGitHttpAuthDataProvider<ServerAccount>> {
        return GitHttpAuthDataProvider.EP_NAME.extensionList
            .filterIsInstance<SilentHostedGitHttpAuthDataProvider<ServerAccount>>()
    }

    fun findGitHttpAuthProvider(providerId: String): SilentHostedGitHttpAuthDataProvider<ServerAccount>? {
        return getGitHttpAuthProviders().find { provider -> provider.providerId == providerId }
    }

    fun getGitHttpAuthAccounts(providerId: String): MutableList<ServerAccount> {
        return findGitHttpAuthProvider(providerId)?.accountManager?.accountsState?.value.orEmpty().toMutableList()
    }

    fun findGitHttpAuthAccount(credentials: String): ServerAccount? {
        val providerId = credentials.substringBefore(':').takeIf(String::isNotBlank) ?: return null
        val accountId = credentials.substringAfter(':').takeIf(String::isNotBlank) ?: return null
        val accountManager = findGitHttpAuthProvider(providerId)?.accountManager ?: return null
        return accountManager.findAccountOrNull { account -> account.id == accountId }
    }

    suspend fun findGitHttpAuthBearerToken(credentials: String): Pair<String, String>? {
        val providerId = credentials.substringBefore(':').takeIf(String::isNotBlank) ?: return null
        val accountId = credentials.substringAfter(':').takeIf(String::isNotBlank) ?: return null
        val accountManager = findGitHttpAuthProvider(providerId)?.accountManager ?: return null
        val account = accountManager.findAccountOrNull { account -> account.id == accountId } ?: return null
        val token = accountManager.findCredentials(account) ?: return null
        return account.name to token
    }

    fun findMavenRepoAuthHeaders(url: String): Map<String, String> {
        val (username, password) = findMavenRepoCredentials(url) ?: return emptyMap()
        return basicAuthHeader(username, password)
    }

    fun findMavenRepoCredentials(url: String): Pair<String, String>? {
        val repoData = MinecraftSettings.instance.creatorMavenRepos.find { url.startsWith(it.url) }
        if (repoData == null) {
            return null
        }

        // First check credentials in IntelliJ IDEA
        if (repoData.username.isNotBlank()) {
            val credentials = getCredentials(repoData.url, repoData.username)
            val username = credentials?.userName
            val password = credentials?.getPasswordAsString()
            if (username != null && password != null) {
                return username to password
            }
        }

        // If IntelliJ doesn't have them look into the Maven settings, or Gradle properties
        val sourcedCredentials = findMavenServerCredentials(repoData.id) ?: findGradleRepoCredentials(repoData.id)
        if (sourcedCredentials != null) {
            return sourcedCredentials
        }

        return null
    }

    fun getMavenSettingsPath(): Path {
        return Path(System.getProperty("user.home"), ".m2", "settings.xml")
    }

    private fun findMavenServerCredentials(serverId: String): Pair<String, String>? {
        val path = getMavenSettingsPath()
        if (!path.exists()) {
            return null
        }

        val document = path.inputStream().use { input -> xmlDocumentBuilder.parse(input) }
        val nodes = xPath.evaluateExpression(
            "/settings/servers/server/id/text()[.=\"$serverId\"]/ancestor::server/*",
            document,
            XPathNodes::class.java
        )

        var username: String? = null
        var password: String? = null
        for (node in nodes) {
            when (node.nodeName) {
                "username" -> username = node.textContent
                "password" -> password = node.textContent
            }
        }

        if (username != null && password != null) {
            return username to password
        }

        return null
    }

    fun getGradleProperties(): Path {
        return System.getenv("GRADLE_USER_HOME")?.let(::Path)
            ?: Path(System.getProperty("user.home"), ".gradle", "gradle.properties")
    }

    private fun findGradleRepoCredentials(id: String): Pair<String, String>? {
        val path = getGradleProperties()
        if (!path.exists()) {
            return null
        }

        val properties = Properties()
        path.inputStream().use(properties::load)

        val username = properties[id + "Username"]?.toString()
        val password = properties[id + "Password"]?.toString()
        if (username != null && password != null) {
            return username to password
        }

        return null
    }
}
