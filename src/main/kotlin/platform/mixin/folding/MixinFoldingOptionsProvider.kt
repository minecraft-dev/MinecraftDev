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
            { b -> settings.state.foldTargetDescriptors = b },
        )
        checkBox("Object casts", { settings.state.foldObjectCasts }, { b -> settings.state.foldObjectCasts = b })
        checkBox(
            "Invoker casts",
            { settings.state.foldInvokerCasts },
            { b -> settings.state.foldInvokerCasts = b },
        )
        checkBox(
            "Invoker method calls",
            { settings.state.foldInvokerMethodCalls },
            { b -> settings.state.foldInvokerMethodCalls = b },
        )
        checkBox(
            "Accessor casts",
            { settings.state.foldAccessorCasts },
            { b -> settings.state.foldAccessorCasts = b },
        )
        checkBox(
            "Accessor method calls",
            { settings.state.foldAccessorMethodCalls },
            { b -> settings.state.foldAccessorMethodCalls = b },
        )
    }
}
