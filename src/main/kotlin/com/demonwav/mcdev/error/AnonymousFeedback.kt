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

    fun sendFeedback(factory: HttpConnectionFactory, envDetails: LinkedHashMap<String, String?>): Int {
        return sendFeedback(factory, convertToGitHubIssueFormat(envDetails))
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

    private fun sendFeedback(factory: HttpConnectionFactory, payload: ByteArray): Int {
        val url = "https://www.demonwav.com/errorReport"
        var userAgent = "Minecraft Development IntelliJ IDEA plugin"

        val pluginDescriptor = PluginManager.getPlugin(PluginUtil.PLUGIN_ID)
        if (pluginDescriptor != null) {
            val name = pluginDescriptor.name
            val version = pluginDescriptor.version
            userAgent = "$name ($version)"
        }

        val connection = connect(factory, url)
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Content-Type", "application/json")
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
        val issueNum = json["number"].toString().toDouble().toInt()
        return issueNum
    }

    private fun connect(factory: HttpConnectionFactory, url: String): HttpURLConnection {
        val connection = factory.openHttpConnection(url)
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection
    }

    open class HttpConnectionFactory {
        open fun openHttpConnection(url: String): HttpURLConnection {
            return URL(url).openConnection() as HttpURLConnection
        }
    }
}
