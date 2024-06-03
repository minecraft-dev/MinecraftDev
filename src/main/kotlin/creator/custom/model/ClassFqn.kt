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

@TemplateApi
data class ClassFqn(val fqn: String) {

    /**
     * The [Class.simpleName] of this class.
     */
    val className by lazy { fqn.substringAfterLast('.') }

    /**
     * The relative filesystem path to this class, without extension.
     */
    val path by lazy { fqn.replace('.', '/') }

    /**
     * The package name of this FQN as it would appear in source code.
     */
    val packageName by lazy { fqn.substringBeforeLast('.') }

    /**
     * The package path of this FQN reflected as a local filesystem path
     */
    val packagePath by lazy { packageName.replace('.', '/') }

    fun withClassName(className: String) = copy("$packageName.$className")

    fun withSubPackage(name: String) = copy("$packageName.$name.$className")

    override fun toString(): String = fqn
}
