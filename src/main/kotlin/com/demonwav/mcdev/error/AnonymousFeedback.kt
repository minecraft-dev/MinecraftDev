/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.error

import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.plugins.PluginManager
import org.apache.commons.io.IOUtils
import java.net.HttpURLConnection
import java.net.URL

object AnonymousFeedback {

    data class FeedbackData(val url: String, val token: Int, val isDuplicate: Boolean)

    const val url = "https://www.demonwav.com/errorReport"

    fun sendFeedback(factory: HttpConnectionFactory, envDetails: LinkedHashMap<String, String?>): FeedbackData {
        val duplicateId = findDuplicateIssue(envDetails)
        if (duplicateId != null) {
            // This is a duplicate
            val commentUrl = sendCommentOnDuplicateIssue(duplicateId, factory, convertToGitHubIssueFormat(envDetails))
            return FeedbackData(commentUrl, duplicateId, true)
        }

        val (htmlUrl, token) = sendFeedback(factory, convertToGitHubIssueFormat(envDetails))
        return FeedbackData(htmlUrl, token, false)
    }

    private fun convertToGitHubIssueFormat(envDetails: LinkedHashMap<String, String?>): ByteArray {
        val result = LinkedHashMap<String, String>(5)
        result.put("title", "[auto-generated] Exception in plugin")
        result.put("body", generateGitHubIssueBody(envDetails))
        return Gson().toJson(result).toByteArray()
    }

    private fun generateGitHubIssueBody(body: LinkedHashMap<String, String?>): String {
        val errorDescription = body["error.description"] ?: ""
        body.remove("error.description")

        var errorMessage = body["error.message"]
        if (errorMessage.isNullOrBlank()) {
            errorMessage = "no error"
        }
        body.remove("error.message")

        var stackTrace = body["error.stacktrace"]
        if (stackTrace.isNullOrEmpty()) {
            stackTrace = "no stacktrace"
        }
        body.remove("error.stacktrace")

        val sb = StringBuilder()

        if (!errorDescription.isEmpty()) {
            sb.append(errorDescription).append("\n\n")
        }

        for ((key, value) in body) {
            sb.append(key).append(": ").append(value).append("\n")
        }

        sb.append("\n```\n").append(stackTrace).append("\n```\n")
        sb.append("\n```\n").append(errorMessage).append("\n```\n")

        return sb.toString()
    }

    private fun sendFeedback(factory: HttpConnectionFactory, payload: ByteArray): Pair<String, Int> {
        val connection = getConnection(factory, url)
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

        val json = Gson().fromJson<Map<*, *>>(body)
        return json["html_url"] as String to (json["id"] as Double).toInt()
    }

    private fun connect(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        val connection = factory.openHttpConnection(url)
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection
    }

    private fun findDuplicateIssue(envDetails: LinkedHashMap<String, String?>): Int? {
        val stack = envDetails["error.stacktrace"] ?: return null

        val text = URL("https://api.github.com/repos/minecraft-dev/MinecraftDev/issues").readText()
        val list = Gson().fromJson<List<Map<*, *>>>(text)
        val block = list.firstOrNull {
            val body = it["body"] as? String ?: return@firstOrNull false

            val first = body.indexOf("\n```\n", startIndex = 0) + 5
            val second = body.indexOf("\n```\n", startIndex = first)
            val stackText = body.substring(first, second)

            stackText == stack
        } ?: return null
        return (block["id"] as Double).toInt()
    }

    private fun sendCommentOnDuplicateIssue(id: Int, factory: HttpConnectionFactory, payload: ByteArray): String {
        val commentUrl = "$url/$id/comments"
        val connection = getConnection(factory, commentUrl)
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

        val json = Gson().fromJson<Map<*, *>>(body)
        return json["html_url"] as String
    }

    private fun getConnection(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        var userAgent = "Minecraft Development IntelliJ IDEA plugin"

        val pluginDescription = PluginManager.getPlugin(PluginUtil.PLUGIN_ID)
        if (pluginDescription != null) {
            val name = pluginDescription.name
            val version = pluginDescription.version
            userAgent = "$name ($version)"
        }

        val connection = connect(factory, url)
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Content-Type", "application/json")

        return connection
    }

    open class HttpConnectionFactory {
        open fun openHttpConnection(url: String): HttpURLConnection {
            return URL(url).openConnection() as HttpURLConnection
        }
    }
}
