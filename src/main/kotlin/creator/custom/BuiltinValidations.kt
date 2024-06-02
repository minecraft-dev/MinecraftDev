package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.text.StringUtil

object BuiltinValidations {
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
        validationErrorIf<String>("Must match regex $regex") { !it.matches(regex) }
}
