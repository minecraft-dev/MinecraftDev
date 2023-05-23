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

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.creator.getText
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.logger

data class SpongeVersion(var versions: LinkedHashMap<String, String>, var selectedIndex: Int) {
    companion object {
        private val LOGGER = logger<SpongeVersion>()

        suspend fun downloadData(): SpongeVersion? {
            return try {
                val text = getText("sponge_v2.json")
                Gson().fromJson(text, SpongeVersion::class)
            } catch (e: Exception) {
                LOGGER.error("Failed to download Sponge version json", e)
                null
            }
        }
    }
}
