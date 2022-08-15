/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.translations.TranslationConstants

enum class Ordering(val text: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending"),
    LIKE_DEFAULT("Like default (${TranslationConstants.DEFAULT_LOCALE})"),
    TEMPLATE("Use Project Template")
}
