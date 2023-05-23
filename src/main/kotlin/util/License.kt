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

package com.demonwav.mcdev.util

enum class License(private val displayName: String, val id: String) {
    MIT("MIT", "MIT"),
    ALL_RIGHTS_RESERVED("All Rights Reserved", "All-Rights-Reserved"),
    ISC("Internet Systems Consortium (ISC) License", "ISC"),
    BSD_2("BSD 2-Clause (FreeBSD) License", "BSD-2-Clause-FreeBSD"),
    BSD_3("BSD 3-Clause (NewBSD) License", "BSD-3-Clause"),
    APACHE_2("Apache 2.0", "Apache-2.0"),
    MPL_2("Mozilla Public License 2.0", "MPL-2.0"),
    LGPL_3("GNU LGPL 3.0", "LGPL-3.0"),
    GPL_3("GNU GPL 3.0", "GPL-3.0"),
    AGPL_3("GNU AGPL 3.0", "AGPL-3.0"),
    UNLICENSE("Unlicense", "unlicense"),

    ;

    override fun toString() = displayName

    companion object {
        private val byId = values().associateBy { it.id }
        fun byId(id: String) = byId[id]
    }
}
