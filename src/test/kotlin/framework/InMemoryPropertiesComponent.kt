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

package com.demonwav.mcdev.framework

import com.intellij.ide.util.PropertiesComponent
import java.util.concurrent.ConcurrentHashMap

/**
 * A [PropertiesComponent] that starts empty and never saves. Register it with `replaceService` to keep values a test
 * stores from reaching later tests.
 */
class InMemoryPropertiesComponent : PropertiesComponent() {
    private val values = ConcurrentHashMap<String, String>()
    private val lists = ConcurrentHashMap<String, List<String>>()

    override fun unsetValue(name: String) {
        values.remove(name)
        lists.remove(name)
    }

    override fun isValueSet(name: String): Boolean = values.containsKey(name) || lists.containsKey(name)

    override fun getValue(name: String): String? = values[name]

    override fun setValue(name: String, value: String?) {
        if (value == null) values.remove(name) else values[name] = value
    }

    override fun setValue(name: String, value: String?, defaultValue: String?) {
        setValue(name, value.takeUnless { it == defaultValue })
    }

    override fun setValue(name: String, value: Float, defaultValue: Float) {
        setValue(name, value.toString(), defaultValue.toString())
    }

    override fun setValue(name: String, value: Int, defaultValue: Int) {
        setValue(name, value.toString(), defaultValue.toString())
    }

    override fun setValue(name: String, value: Boolean, defaultValue: Boolean) {
        setValue(name, value.toString(), defaultValue.toString())
    }

    override fun updateValue(name: String, value: Boolean): Boolean =
        values.put(name, value.toString()) != value.toString()

    override fun getValues(name: String): Array<String>? = lists[name]?.toTypedArray()

    override fun setValues(name: String, values: Array<String>?) {
        setList(name, values?.toList())
    }

    override fun getList(name: String): List<String>? = lists[name]

    override fun setList(name: String, values: Collection<String>?) {
        if (values.isNullOrEmpty()) lists.remove(name) else lists[name] = values.toList()
    }
}
