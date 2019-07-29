/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.sorting

import com.demonwav.mcdev.i18n.I18nConstants

enum class Ordering(val text: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending"),
    LIKE_DEFAULT("Like default (${I18nConstants.DEFAULT_LOCALE_FILE})"),
    TEMPLATE("Use Project Template")
}
