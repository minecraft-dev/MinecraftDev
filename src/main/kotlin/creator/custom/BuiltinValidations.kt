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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.text.StringUtil
import javax.swing.JComponent

object BuiltinValidations {
    val nonBlank = validationErrorIf<String>(MCDevBundle("creator.validation.blank")) { it.isBlank() }

    val validVersion = validationErrorIf<String>(MCDevBundle("creator.validation.semantic_version")) {
        SemanticVersion.tryParse(it) == null
    }

    val nonEmptyVersion = DialogValidation.WithParameter<ComboBox<SemanticVersion>> { combobox ->
        DialogValidation {
            if (combobox.item?.parts.isNullOrEmpty()) {
                ValidationInfo(MCDevBundle("creator.validation.semantic_version"))
            } else {
                null
            }
        }
    }

    val nonEmptyYarnVersion = DialogValidation.WithParameter<ComboBox<FabricVersions.YarnVersion>> { combobox ->
        DialogValidation {
            if (combobox.item == null) {
                ValidationInfo(MCDevBundle("creator.validation.semantic_version"))
            } else {
                null
            }
        }
    }

    val validClassFqn = validationErrorIf<String>(MCDevBundle("creator.validation.class_fqn")) {
        it.isBlank() || it.split('.').any { part -> !StringUtil.isJavaIdentifier(part) }
    }

    fun byRegex(regex: Regex): DialogValidation.WithParameter<() -> String> =
        validationErrorIf<String>(MCDevBundle("creator.validation.regex", regex)) { !it.matches(regex) }

    fun <T> isAnyOf(
        selectionGetter: () -> T,
        options: Collection<T>,
        component: JComponent? = null
    ): DialogValidation = DialogValidation {
        if (selectionGetter() !in options) {
            return@DialogValidation ValidationInfo(MCDevBundle("creator.validation.invalid_option"), component)
        }

        return@DialogValidation null
    }
}
