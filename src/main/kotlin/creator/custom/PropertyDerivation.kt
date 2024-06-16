package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.creator.custom.types.CreatorProperty

fun interface PreparedDerivation {
    fun derive(parentValues: List<Any?>): Any?
}

interface PropertyDerivationFactory {

    fun create(
        reporter: TemplateValidationReporter,
        parents: List<CreatorProperty<*>?>?,
        derivation: PropertyDerivation
    ): PreparedDerivation?
}
