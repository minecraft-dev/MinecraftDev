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

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.util.ExceptionUtil

// It's easier to just re-use the code that we already were using, rather than changing to a map like
// Jetbrains said to do in the deprecation message
class ErrorBean(throwable: Throwable?, val lastAction: String?) {

    val stackTrace: String? = if (throwable != null) ExceptionUtil.getThrowableText(throwable) else null
    var message: String? = null
    var description: String? = null
    var pluginName: String? = null
    var pluginVersion: String? = null
    var attachments = emptyList<Attachment>()

    init {
        if (throwable != null) {
            message = throwable.message
        }
    }
}
