/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.errorreporter

data class LinkedStackTraceElement(val stackElementText: String, val httpLinkText: String) {
    fun apply(text: String): String {
        return text.replace(stackElementText, httpLinkText)
    }
}
