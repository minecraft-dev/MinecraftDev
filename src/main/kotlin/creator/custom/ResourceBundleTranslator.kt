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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.openapi.util.text.StringUtil
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

abstract class ResourceBundleTranslator {

    abstract val bundle: ResourceBundle?

    fun translate(key: @NonNls String): @Nls String {
        return translateOrNull(key) ?: StringUtil.escapeMnemonics(key)
    }

    fun translateOrNull(key: @NonNls String): @Nls String? {
        if (bundle != null) {
            try {
                return bundle!!.getString(key)
            } catch (_: MissingResourceException) {
            }
        }
        return MCDevBundle.messageOrNull(key)
    }
}
