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

package com.demonwav.mcdev.platform.fabric.annotater

import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocStore
import java.nio.file.Path

/**
 * @property managed whether the plugin created this checkout and may run git operations in it
 */
class AnnotaterCheckout(val root: Path, val config: AnnotaterConfig, val managed: Boolean) {
    val version: String
        get() = config.version

    val mappingsDir: Path = root.resolve(config.mappings)

    val javadocStore = AnnotaterJavadocStore.forDirectory(mappingsDir)
}
