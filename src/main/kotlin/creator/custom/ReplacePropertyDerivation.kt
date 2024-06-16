package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.creator.custom.types.CreatorProperty

class ReplacePropertyDerivation(
    val regex: Regex,
    val replacement: String,
    val maxLength: Int?,
) : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any? {
        val projectName = parentValues.first() as? String
            ?: return null

        val sanitized = projectName.lowercase().replace(regex, replacement)
        if (maxLength != null && sanitized.length > maxLength) {
            return sanitized.substring(0, maxLength)
        }

        return sanitized
    }

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (derivation.parameters == null) {
                reporter.error("Missing parameters")
                return null
            }

            if (parents.isNullOrEmpty()) {
                reporter.error("Missing parent value")
                return null
            }

            if (parents.size > 2) {
                reporter.warn("More than one parent defined")
            }

            if (parents.first()?.get() !is String) {
                reporter.error("Parent property must produce a string value")
                return null
            }

            val regexString = derivation.parameters["regex"] as? String
            if (regexString == null) {
                reporter.error("Missing 'regex' string parameter")
                return null
            }

            val regex = try {
                Regex(regexString)
            } catch (t: Throwable) {
                reporter.error("Invalid regex: '$regexString': ${t.message}")
                return null
            }

            val replacement = derivation.parameters["replacement"] as? String
            if (replacement == null) {
                reporter.error("Missing 'replacement' string parameter")
                return null
            }

            val maxLength = (derivation.parameters["maxLength"] as? Number)?.toInt()
            return ReplacePropertyDerivation(regex, replacement, maxLength)
        }
    }
}
