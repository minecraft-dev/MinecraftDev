/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.asset

import com.intellij.DynamicBundle
import java.util.function.Supplier
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.MinecraftDevelopment"

object MCDevBundle : DynamicBundle(BUNDLE) {

    operator fun invoke(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
        return getMessage(key)
    }

    operator fun invoke(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
        return getMessage(key, *params)
    }

    fun pointer(@PropertyKey(resourceBundle = BUNDLE) key: String) = Supplier { invoke(key) }

    fun pointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        Supplier { invoke(key, params) }
}
