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

package com.demonwav.mcdev.creator.custom.npm

import java.net.URLEncoder

// The HTTP fixture recorder runs outside the IDE and uses this to name the npm fixtures, so this file must only use the
// JDK and the Kotlin stdlib
object NpmRegistry {
    private const val URL = "https://registry.npmjs.org"

    /**
     * The registry URL for a package's metadata at [version], which may be a version or a dist-tag.
     */
    fun packageUrl(name: String, version: String): String =
        // Scoped packages need their slash to be escaped
        "$URL/${name.replace("/", "%2F")}/${encodeURIComponent(version)}"

    // Same result as the platform's URLUtil.encodeURIComponent
    private fun encodeURIComponent(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
}
