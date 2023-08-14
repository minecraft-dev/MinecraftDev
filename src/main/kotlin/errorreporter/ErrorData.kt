/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.errorreporter

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil

// It's easier to just re-use the code that we already were using, rather than changing to a map like
// Jetbrains said to do in the deprecation message
class ErrorData(var throwable: Throwable?, private val lastAction: String?) {

    var message: String? = null
        get() = field ?: throwable?.message

    var description: String? = null
    var pluginName: String? = null
    var pluginVersion: String? = null
    var attachments = emptyList<Attachment>()

    private val versionRegex by lazy(LazyThreadSafetyMode.NONE) {
        Regex("""(?<intellijVersion>\d{4}\.\d)-(?<pluginVersion>\d+\.\d+\.\d+)""")
    }

    fun formatErrorData(): Pair<LinkedHashMap<String, String?>, List<Attachment>> {
        val appInfo = ApplicationInfoEx.getInstanceEx()
        val namesInfo = ApplicationNamesInfo.getInstance()

        val params = LinkedHashMap<String, String?>(21)

        params["error.description"] = description

        params["Plugin Name"] = pluginName
        params["Plugin Version"] = pluginVersion

        params["OS Name"] = SystemInfo.OS_NAME
        params["Java Version"] = SystemInfo.JAVA_VERSION
        params["Java VM Vendor"] = SystemInfo.JAVA_VENDOR

        params["App Name"] = namesInfo.productName
        params["App Full Name"] = namesInfo.fullProductName
        params["App Version Name"] = appInfo.versionName
        params["Is EAP"] = appInfo.isEAP.toString()
        params["App Build"] = appInfo.build.asString()
        params["App Version"] = appInfo.fullVersion

        if (lastAction.isNullOrBlank()) {
            params["Last Action"] = "None"
        } else {
            params["Last Action"] = lastAction
        }

        params["error.message"] = message
        params["error.raw_stacktrace"] = throwable?.stackTraceToString()
        params["error.stacktrace"] = formatStackTrace()

        return params to attachments
    }

    private fun formatStackTrace(): String? {
        val t = throwable ?: return null
        val stackText = escape(t.stackTraceToString())

        val version = pluginVersion ?: return stackText
        val match = versionRegex.matchEntire(version) ?: return stackText

        val intellijVersion = match.groups["intellijVersion"]?.value ?: return stackText
        val pluginVersion = match.groups["pluginVersion"]?.value ?: return stackText

        val tag = "$pluginVersion-$intellijVersion"
        val baseTagUrl = "https://github.com/minecraft-dev/MinecraftDev/blob/$tag/src/main/kotlin/"

        val links = mutableListOf<LinkedStackTraceElement>()

        val mcdevPackage = "com.demonwav.mcdev"

        for (element in t.stackTrace) {
            if (!element.className.startsWith(mcdevPackage)) {
                continue
            }

            val path = element.className.substring(mcdevPackage.length + 1).substringBeforeLast('.').replace('.', '/')
            val file = element.fileName
            val line = element.lineNumber

            val escapedElement = escape(element)
            val linkText = "<a href=\"$baseTagUrl$path/$file#L$line\">$escapedElement</a>"
            links += LinkedStackTraceElement(escapedElement, linkText)
        }

        if (links.isEmpty()) {
            return stackText
        }

        var currentStackText = stackText
        for (link in links) {
            currentStackText = link.apply(currentStackText)
        }

        return currentStackText
    }

    private fun escape(text: Any) = StringUtil.escapeXmlEntities(text.toString())
}
