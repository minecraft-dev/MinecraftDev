/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.util.net.HttpConfigurable
import java.net.HttpURLConnection
import java.net.URL

sealed class HttpConnectionFactory {
    open fun openHttpConnection(url: String) = URL(url).openConnection() as HttpURLConnection
}

object ProxyHttpConnectionFactory : HttpConnectionFactory() {
    override fun openHttpConnection(url: String) = HttpConfigurable.getInstance().openHttpConnection(url)
}
