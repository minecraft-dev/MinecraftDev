/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.options.BeanConfigurable

class MixinFoldingOptionsProvider :
    BeanConfigurable<MixinFoldingSettings.State>(MixinFoldingSettings.instance.state), CodeFoldingOptionsProvider {

    init {
        val settings = MixinFoldingSettings.instance
        checkBox(
            "Mixin: Target descriptors",
            { settings.state.foldTargetDescriptors },
            { b -> settings.state.foldTargetDescriptors = b })
        checkBox("Mixin: Object casts", { settings.state.foldObjectCasts }, { b -> settings.state.foldObjectCasts = b })
    }
}
