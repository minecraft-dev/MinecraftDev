package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.thisLogger

class SelectPropertyDerivation(
    val parents: List<String>?,
    val options: List<PropertyDerivationSelect>,
    val default: Any?,
) : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any? {
        val properties = if (!parents.isNullOrEmpty()) {
            parentValues.mapIndexed { i, value -> parents[i] to value }.toMap()
        } else {
            emptyMap()
        }
        for (option in options) {
            if (TemplateEvaluator.condition(properties, option.condition).getOrLogException(thisLogger()) == true) {
                return option.value
            }
        }

        return default
    }

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (derivation.select == null) {
                reporter.error("Missing select options")
                return null
            }

            return SelectPropertyDerivation(derivation.parents, derivation.select, derivation.default)
        }
    }
}
