/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.error

import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.HttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.util.io.readCharSequence
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import org.apache.commons.io.IOUtils

object AnonymousFeedback {

    data class FeedbackData(val url: String, val token: Int, val isDuplicate: Boolean)

    const val url = "https://www.demonwav.com/errorReport"

    fun sendFeedback(
        factory: HttpConnectionFactory,
        envDetails: LinkedHashMap<String, String?>,
        attachments: List<Attachment>
    ): FeedbackData {
        val duplicateId = findDuplicateIssue(envDetails, factory)
        if (duplicateId != null) {
            // This is a duplicate
            val commentUrl =
                sendCommentOnDuplicateIssue(duplicateId, factory, convertToGitHubIssueFormat(envDetails, attachments))
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

        var stackTrace = body.remove("error.stacktrace")
        if (stackTrace.isNullOrEmpty()) {
            stackTrace = "no stacktrace"
        }

        val sb = StringBuilder()

        if (!errorDescription.isEmpty()) {
            sb.append(errorDescription).append("\n\n")
        }

        for ((key, value) in body) {
            sb.append(key).append(": ").append(value).append("\n")
        }

        sb.append("\n```\n").append(stackTrace).append("\n```\n")
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
        val connection = getConnection(factory, url)
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

    private val numberRegex = Regex("\\d+")
    private val newLineRegex = Regex("[\r\n]+")

    private const val openIssueUrl = "https://api.github.com/repos/minecraft-dev/MinecraftDev/issues" +
        "?state=open&creator=minecraft-dev-autoreporter&per_page=100"
    private const val closedIssueUrl = "https://api.github.com/repos/minecraft-dev/MinecraftDev/issues" +
        "?state=closed&creator=minecraft-dev-autoreporter&per_page=100"

    private const val packagePrefix = "\tat com.demonwav.mcdev"

    private fun findDuplicateIssue(envDetails: LinkedHashMap<String, String?>, factory: HttpConnectionFactory): Int? {
        val stack = envDetails["error.stacktrace"]?.replace(numberRegex, "") ?: return null

        val stackMcdevParts = stack.lineSequence()
            .filter { line -> line.startsWith(packagePrefix) }
            .joinToString("\n")

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
            val stackText = body.substring(first, second)

            val mcdevParts = stackText.lineSequence()
                .filter { line -> line.startsWith(packagePrefix) }
                .joinToString("\n")

            return stackMcdevParts == mcdevParts
        }

        // Look first for an open issue, then for a closed issue if one isn't found
        val block = getAllIssues(openIssueUrl, factory)?.firstOrNull(predicate)
            ?: getAllIssues(closedIssueUrl, factory)?.firstOrNull(predicate)
            ?: return null
        return (block["number"] as Double).toInt()
    }

    private fun getAllIssues(url: String, factory: HttpConnectionFactory): List<Map<*, *>>? {
        var connection = connect(factory, url)
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", userAgent)

        connection.connect()
        if (connection.responseCode != 200) {
            connection.disconnect()
            return null
        }

        val list = mutableListOf<Map<*, *>>()
        var data = connection.inputStream.reader().use(InputStreamReader::readCharSequence).toString()

        var response = Gson().fromJson<List<Map<*, *>>>(data)
        list.addAll(response)

        var link = connection.getHeaderField("Link")
        connection.disconnect()

        var next = getNextLink(link)
        while (next != null) {
            connection = connect(factory, next)
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)

            connection.connect()
            if (connection.responseCode != 200) {
                connection.disconnect()
                continue
            }

            data = connection.inputStream.reader().use(InputStreamReader::readCharSequence).toString()

            response = Gson().fromJson(data)
            list.addAll(response)

            link = connection.getHeaderField("Link")
            connection.disconnect()
            next = getNextLink(link)
        }

        return list
    }

    private fun getNextLink(link: String?): String? {
        if (link == null) {
            return null
        }
        val lines = link.split(",")
        for (line in lines) {
            if (!line.contains("rel=\"next\"")) {
                continue
            }

            val parts = line.split(";")
            return parts[0].substring(1, parts[0].length - 1)
        }

        return null
    }

    private fun sendCommentOnDuplicateIssue(id: Int, factory: HttpConnectionFactory, payload: ByteArray): String {
        val commentUrl = "$url/$id/comments"
        val connection = getConnection(factory, commentUrl)
        val json = executeCall(connection, payload)
        return json["html_url"] as String
    }

    private fun executeCall(connection: HttpURLConnection, payload: ByteArray): Map<*, *> {
        connection.outputStream.use {
            it.write(payload)
        }

        val responseCode = connection.responseCode
        if (responseCode != 201) {
            throw RuntimeException("Expected HTTP_CREATED (201), obtained $responseCode instead.")
        }

        val contentEncoding = connection.contentEncoding ?: "UTF-8"
        val body = connection.inputStream.use {
            IOUtils.toString(it, contentEncoding)
        }
        connection.disconnect()

        return Gson().fromJson<Map<*, *>>(body)
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

        val pluginDescription = PluginManager.getPlugin(PluginUtil.PLUGIN_ID)
        if (pluginDescription != null) {
            val name = pluginDescription.name
            val version = pluginDescription.version
            agent = "$name ($version)"
        }
        agent
    }
}
