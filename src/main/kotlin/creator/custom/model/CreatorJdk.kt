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

package com.demonwav.mcdev.creator.custom.model

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk

@TemplateApi
data class CreatorJdk(val sdk: Sdk?) {

    val javaVersion: Int
        get() = sdk?.let { JavaSdk.getInstance().getVersion(it)?.ordinal } ?: 17
}
