/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.options.BeanConfigurable

class MixinFoldingOptionsProvider :
    BeanConfigurable<MixinFoldingSettings.State>(MixinFoldingSettings.instance.state), CodeFoldingOptionsProvider {

    init {
        title = "Mixin"

        val settings = MixinFoldingSettings.instance
        checkBox(
            "Target descriptors",
            { settings.state.foldTargetDescriptors },
            { b -> settings.state.foldTargetDescriptors = b }
        )
        checkBox("Object casts", { settings.state.foldObjectCasts }, { b -> settings.state.foldObjectCasts = b })
        checkBox(
            "Invoker casts",
            { settings.state.foldInvokerCasts },
            { b -> settings.state.foldInvokerCasts = b }
        )
        checkBox(
            "Invoker method calls",
            { settings.state.foldInvokerMethodCalls },
            { b -> settings.state.foldInvokerMethodCalls = b }
        )
        checkBox(
            "Accessor casts",
            { settings.state.foldAccessorCasts },
            { b -> settings.state.foldAccessorCasts = b }
        )
        checkBox(
            "Accessor method calls",
            { settings.state.foldAccessorMethodCalls },
            { b -> settings.state.foldAccessorMethodCalls = b }
        )
    }
}
