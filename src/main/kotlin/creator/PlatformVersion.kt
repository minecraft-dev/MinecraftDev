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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.withSuppressed
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.proxy.CommonProxy
import java.io.IOException
import java.net.Proxy
import java.net.URI
import kotlin.reflect.KClass

// Cloudflare and GitHub are both global CDNs
// Cloudflare is used first / preferred simply due to domain preference
private const val CLOUDFLARE_BASE_URL = "https://mcdev.io/versions/"
// Directly retrieving the file via GitHub is the second option. In some regions / networks Cloudflare is blocked,
// but we may still be able to reach GitHub
private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/minecraft-dev/minecraftdev.org/master/versions/"
// Finally, there are apparently also regions / networks where both Cloudflare and GitHub is blocked.
// Or maybe the domain `mcdev.io` (and prior to that, `minecraftdev.org`) is blocked due to weird domain
// rules (perhaps blocking on the word "minecraft"). In one last ditch effort to retrieve the version json
// we can also pull from this host, a separate host using a separate domain. This is an OVH server, not
// proxied through Cloudflare.
private const val OVH_BASE_URL = "https://versions.denwav.com/versions/"

private val URLS = listOf(CLOUDFLARE_BASE_URL, GITHUB_BASE_URL, OVH_BASE_URL)

val PLATFORM_VERSION_LOGGER = logger<PlatformVersion>()

suspend fun getVersionSelector(type: PlatformType): PlatformVersion {
    val versionJson = type.versionJson ?: throw UnsupportedOperationException("Incorrect platform type: $type")
    return getVersionJson(versionJson)
}

suspend inline fun <reified T : Any> getVersionJson(path: String): T {
    return getVersionJson(path, T::class)
}

suspend fun <T : Any> getVersionJson(path: String, type: KClass<T>): T {
    val text = getText(path)
    try {
        return Gson().fromJson(text, type)
    } catch (e: Exception) {
        val attachment = Attachment("JSON Document", text)
        attachment.isIncluded = true
        PLATFORM_VERSION_LOGGER.error("Failed to parse JSON document from '$path'", e, attachment)
        throw e
    }
}

suspend fun getText(path: String): String {
    var thrown: Exception? = null
    return URLS.mapFirstNotNull { url ->
        try {
            doCall(url + path)
        } catch (e: Exception) {
            PLATFORM_VERSION_LOGGER.warn("Failed to reach URL $url$path")
            thrown = withSuppressed(thrown, e)
            null
        }
    } ?: throw thrown!!
}

private suspend fun doCall(urlText: String): String {
    val manager = FuelManager()
    manager.proxy = selectProxy(urlText)

    return manager.get(urlText)
        .header("User-Agent", "github_org/minecraft-dev/${PluginUtil.pluginVersion}")
        .header("Accepts", "application/json")
        .suspendable()
        .awaitString()
}

fun selectProxy(urlText: String): Proxy? {
    val uri = URI(urlText)
    val url = uri.toURL()

    val proxies = CommonProxy.getInstance().select(uri)
    for (proxy in proxies) {
        try {
            url.openConnection(proxy)
            return proxy
        } catch (_: IOException) {}
    }
    return null
}

data class PlatformVersion(var versions: List<String>, var selectedIndex: Int)
