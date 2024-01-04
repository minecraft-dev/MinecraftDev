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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

class InjectionPointAnnotation : KeyedLazyInstance<String> {
    @Attribute("annotation")
    @RequiredElement
    lateinit var annotation: String

    @Attribute("atCode")
    @RequiredElement
    lateinit var atCode: String

    override fun getKey() = annotation

    override fun getInstance() = atCode

    companion object {
        private val EP_NAME = ExtensionPointName<InjectionPointAnnotation>(
            "com.demonwav.minecraft-dev.injectionPointAnnotation"
        )
        private val COLLECTOR = KeyedExtensionCollector<String, String>(EP_NAME)

        fun atCodeFor(qualifiedName: String): String? = COLLECTOR.findSingle(qualifiedName)
    }
}
