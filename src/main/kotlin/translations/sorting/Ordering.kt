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

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.translations.TranslationConstants
import java.util.function.Supplier

enum class Ordering(private val textSupplier: Supplier<String>) {
    ASCENDING(MCDevBundle.pointer("translation_sort.order.ascending")),
    DESCENDING(MCDevBundle.pointer("translation_sort.order.descending")),
    LIKE_DEFAULT(MCDevBundle.pointer("translation_sort.order.like_default", TranslationConstants.DEFAULT_LOCALE)),
    TEMPLATE(MCDevBundle.pointer("translation_sort.order.template"));

    val text
        get() = textSupplier.get()
}
