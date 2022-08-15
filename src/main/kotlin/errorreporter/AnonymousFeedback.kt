/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.errorreporter

import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.HttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.util.text.StringUtil
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.io.IOUtils
import org.apache.http.HttpHeaders
import org.apache.http.entity.ContentType

object AnonymousFeedback {

    data class FeedbackData(val url: String, val token: Int, val isDuplicate: Boolean)

    private const val authedUrl = "https://www.denwav.dev/errorReport"
    private const val baseUrl = "https://api.github.com/repos/minecraft-dev/mcdev-error-report/issues"

    fun sendFeedback(
        factory: HttpConnectionFactory,
        envDetails: LinkedHashMap<String, String?>,
        attachments: List<Attachment>
    ): FeedbackData {
        val duplicateId = findDuplicateIssue(envDetails, factory)
        if (duplicateId != null) {
            // This is a duplicate
            val issueContent = convertToGitHubIssueFormat(envDetails, attachments)
            val commentUrl = sendCommentOnDuplicateIssue(duplicateId, factory, issueContent)
            return FeedbackData(commentUrl, duplicateId, true)
        }

        val (htmlUrl, token) = sendFeedback(factory, convertToGitHubIssueFormat(envDetails, attachments))
        return FeedbackData(htmlUrl, token, false)
    }

    private fun convertToGitHubIssueFormat(
        envDetails: LinkedHashMap<String, String?>,
        attachments: List<Attachment>
    ): ByteArray {
        val result = LinkedHashMap<String, String>(5)
        result["title"] = "[auto-generated] Exception in plugin"
        result["body"] = generateGitHubIssueBody(envDetails, attachments)
        return Gson().toJson(result).toByteArray()
    }

    private fun generateGitHubIssueBody(body: LinkedHashMap<String, String?>, attachments: List<Attachment>): String {
        val errorDescription = body.remove("error.description") ?: ""

        var errorMessage = body.remove("error.message")
        if (errorMessage.isNullOrBlank()) {
            errorMessage = "no error"
        }

        val rawStackTrace = body.remove("error.raw_stacktrace")?.takeIf { it.isNotBlank() } ?: "no stacktrace"
        val stackTrace = body.remove("error.stacktrace")?.takeIf { it.isNotBlank() } ?: "no stacktrace"

        val sb = StringBuilder()

        if (errorDescription.isNotEmpty()) {
            sb.append(errorDescription).append("\n\n")
        }

        sb.append("<table><tr><td><table>\n")
        for ((i, entry) in body.entries.withIndex()) {
            if (i == 6) {
                sb.append("</table></td><td><table>\n")
            }
            val (key, value) = entry
            sb.append("<tr><td><b>")
                .append(key)
                .append("</b></td><td><code>")
                .append(value)
                .append("</code></td></tr>\n")
        }
        sb.append("</table></td></tr></table>\n")

        sb.append("\n<pre><code>").append(stackTrace).append("</code></pre>\n")

        sb.append("\n<details><summary>Original stack trace</summary>\n\n```\n")
            .append(rawStackTrace)
            .append("\n```\n</details>\n")

        sb.append("\n```\n").append(errorMessage).append("\n```\n")

        if (attachments.isNotEmpty()) {
            for (attachment in attachments) {
                sb.append("\n---\n\n```\n").append(attachment.name).append("\n```\n")
                sb.append("```\n")

                try {
                    // No clue what the data format of the attachment is
                    // but if we try to decode it as UTF-8 and it succeeds, chances are likely that's what it is
                    val charBuf = Charsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(attachment.bytes))

                    val text = charBuf.toString()
                    if (text != attachment.displayText) {
                        sb.append(attachment.displayText).append("\n```\n")
                        sb.append("```\n")
                    }
                    sb.append(text)
                } catch (e: Exception) {
                    // Guess it's not text...
                    sb.append(attachment.displayText).append("\n```\n")
                    sb.append("```\n")
                    sb.append(attachment.encodedBytes)
                }

                sb.append("\n```\n")
            }
        }

        return sb.toString()
    }

    private fun sendFeedback(factory: HttpConnectionFactory, payload: ByteArray): Pair<String, Int> {
        val connection = getConnection(factory, authedUrl)
        connection.connect()
        val json = executeCall(connection, payload)
        return json["html_url"] as String to (json["number"] as Double).toInt()
    }

    private fun connect(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        val connection = factory.openHttpConnection(url)
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection
    }

    private const val openIssueUrl = "$baseUrl?state=open&creator=minecraft-dev-autoreporter&per_page=100"
    private const val closedIssueUrl = "$baseUrl?state=closed&creator=minecraft-dev-autoreporter&per_page=100"

    private const val packagePrefix = "\tat com.demonwav.mcdev"

    private fun findDuplicateIssue(envDetails: LinkedHashMap<String, String?>, factory: HttpConnectionFactory): Int? {
        val numberRegex = Regex("\\d+")
        val newLineRegex = Regex("[\r\n]+")

        val stack = envDetails["error.raw_stacktrace"]?.replace(numberRegex, "") ?: return null

        val ourMcdevParts = stack.lineSequence()
            .filter { line -> line.startsWith(packagePrefix) }
            .map { it.trim() }
            .toList()

        if (ourMcdevParts.isEmpty()) {
            return null
        }

        val predicate = fun(map: Map<*, *>): Boolean {
            val body = (map["body"] as? String ?: return false)
                .replace(numberRegex, "")
                .replace(newLineRegex, "\n")

            // We can't comment on locked issues
            if (map["locked"] as Boolean) {
                return false
            }

            val first = body.indexOf("\n```\n", startIndex = 0) + 5
            val second = body.indexOf("\n```\n", startIndex = first)
            if (first == 4 || second == -1) {
                return false
            }

            val stackText = body.substring(first, second)

            val theirMcdevParts = stackText.lineSequence()
                .filter { line -> line.startsWith(packagePrefix) }
                .map { it.trim() }
                .toList()

            return ourMcdevParts == theirMcdevParts
        }

        // Look first for an open issue, then for a closed issue if one isn't found
        val block = getAllIssues(openIssueUrl, factory)?.firstOrNull(predicate)
            ?: getAllIssues(closedIssueUrl, factory, limit = 300)?.firstOrNull(predicate)
            ?: return null
        return (block["number"] as Double).toInt()
    }

    private fun getMcdevStackElementLines(stack: String, numberRegex: Regex, linkRegex: Regex): List<String> {
        return stack.lineSequence()
            .mapNotNull { line -> linkRegex.matchEntire(line)?.groups?.get("content")?.value }
            .map { line -> StringUtil.unescapeXmlEntities(line) }
            .map { line -> line.replace(numberRegex, "") }
            .toList()
    }

    private fun getAllIssues(url: String, factory: HttpConnectionFactory, limit: Int = -1): List<Map<*, *>>? {
        var useAuthed = false

        var next: String? = url
        val list = mutableListOf<Map<*, *>>()

        while (next != null) {
            val connection: HttpURLConnection = connect(factory, next)
            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", userAgent)

                connection.connect()

                if (connection.responseCode == HttpStatus.SC_FORBIDDEN && !useAuthed) {
                    useAuthed = true
                    next = replaceWithAuth(next)
                    continue
                }

                if (connection.responseCode != HttpStatus.SC_OK) {
                    return null
                }

                val charset = connection.getHeaderField(HttpHeaders.CONTENT_TYPE)?.let {
                    ContentType.parse(it).charset
                } ?: Charsets.UTF_8

                val data = connection.inputStream.reader(charset).readText()

                val response = Gson().fromJson<List<Map<*, *>>>(data)
                list.addAll(response)

                if (limit > 0 && list.size >= limit) {
                    return list
                }

                val link = connection.getHeaderField("Link")

                next = getNextLink(link, useAuthed)
            } finally {
                connection.disconnect()
            }
        }

        return list
    }

    private fun getNextLink(linkHeader: String?, useAuthed: Boolean): String? {
        if (linkHeader == null) {
            return null
        }
        val links = linkHeader.split(",")
        for (link in links) {
            if (!link.contains("rel=\"next\"")) {
                continue
            }

            val parts = link.split(";")
            if (parts.isEmpty()) {
                continue
            }
            val nextUrl = parts[0].trim().removePrefix("<").removeSuffix(">")
            if (!useAuthed) {
                return nextUrl
            }

            return replaceWithAuth(nextUrl)
        }

        return null
    }

    private fun replaceWithAuth(url: String): String? {
        // non-authed-API requests are rate limited at 60 / hour / IP
        // authed requests have a rate limit of 5000 / hour / account
        // We don't want to use the authed URL by default since all users would use the same rate limit
        // but it's a good fallback when the non-authed API stops working.
        val index = url.indexOf('?')
        if (index == -1) {
            return null
        }
        return authedUrl + url.substring(index)
    }

    private fun sendCommentOnDuplicateIssue(id: Int, factory: HttpConnectionFactory, payload: ByteArray): String {
        val commentUrl = "$authedUrl/$id/comments"
        val connection = getConnection(factory, commentUrl)
        val json = executeCall(connection, payload)
        return json["html_url"] as String
    }

    private fun executeCall(connection: HttpURLConnection, payload: ByteArray): Map<*, *> {
        connection.outputStream.use {
            it.write(payload)
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpStatus.SC_CREATED) {
            throw RuntimeException("Expected HTTP_CREATED (201), obtained $responseCode instead.")
        }

        val contentEncoding = connection.contentEncoding ?: "UTF-8"
        val body = connection.inputStream.use {
            IOUtils.toString(it, contentEncoding)
        }
        connection.disconnect()

        return Gson().fromJson(body)
    }

    private fun getConnection(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        val connection = connect(factory, url)
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Content-Type", "application/json")

        return connection
    }

    private val userAgent by lazy {
        var agent = "Minecraft Development IntelliJ IDEA plugin"

        val pluginDescription = PluginManagerCore.getPlugin(PluginUtil.PLUGIN_ID)
        if (pluginDescription != null) {
            val name = pluginDescription.name
            val version = pluginDescription.version
            agent = "$name ($version)"
        }
        agent
    }
}
