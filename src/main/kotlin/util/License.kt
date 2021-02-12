/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
}
