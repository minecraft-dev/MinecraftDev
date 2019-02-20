/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.error

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.util.SystemInfo

object IdeaITNProxy {
    fun getKeyValuePairs(
        error: ErrorBean,
        appInfo: ApplicationInfoEx,
        namesInfo: ApplicationNamesInfo
    ): Pair<LinkedHashMap<String, String?>, List<Attachment>> {
        val params = LinkedHashMap<String, String?>(21)

        params["error.description"] = error.description

        params["Plugin Name"] = error.pluginName
        params["Plugin Version"] = error.pluginVersion

        params["OS Name"] = SystemInfo.OS_NAME
        params["Java Version"] = SystemInfo.JAVA_VERSION
        params["Java VM Vendor"] = SystemInfo.JAVA_VENDOR

        params["App Name"] = namesInfo.productName
        params["App Full Name"] = namesInfo.fullProductName
        params["App Version Name"] = appInfo.versionName
        params["Is EAP"] = appInfo.isEAP.toString()
        params["App Build"] = appInfo.build.asString()
        params["App Version"] = appInfo.fullVersion

        if (error.lastAction.isNullOrBlank()) {
            params["Last Action"] = "None"
        } else {
            params["Last Action"] = error.lastAction
        }

        params["error.message"] = error.message
        params["error.stacktrace"] = error.stackTrace

        return params to error.attachments
    }
}
