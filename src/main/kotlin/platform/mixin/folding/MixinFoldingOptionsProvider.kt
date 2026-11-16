/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.options.BeanConfigurable

class MixinFoldingOptionsProvider :
    BeanConfigurable<MixinFoldingSettings.State>(MixinFoldingSettings.instance.state), CodeFoldingOptionsProvider {

    init {
        title = MCDevBundle("mixin.folding.title")

        val settings = MixinFoldingSettings.instance
        checkBox(
            MCDevBundle("mixin.folding.target_descriptors"),
            { settings.state.foldTargetDescriptors },
            { b -> settings.state.foldTargetDescriptors = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.object_casts"),
            { settings.state.foldObjectCasts },
            { b -> settings.state.foldObjectCasts = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.invoker_casts"),
            { settings.state.foldInvokerCasts },
            { b -> settings.state.foldInvokerCasts = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.invoker_method_calls"),
            { settings.state.foldInvokerMethodCalls },
            { b -> settings.state.foldInvokerMethodCalls = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.accessor_casts"),
            { settings.state.foldAccessorCasts },
            { b -> settings.state.foldAccessorCasts = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.accessor_method_calls"),
            { settings.state.foldAccessorMethodCalls },
            { b -> settings.state.foldAccessorMethodCalls = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.expression_definitions"),
            { settings.state.foldDefinitions },
            { b -> settings.state.foldDefinitions = b },
        )
        checkBox(
            MCDevBundle("mixin.folding.expression_definition_members"),
            { settings.state.foldDefinitionFieldsAndMethods },
            { b -> settings.state.foldDefinitionFieldsAndMethods = b },
        )
    }
}
