package com.demonwav.mcdev.creator.custom.model

@TemplateApi
data class StringList(val values: List<String>) : List<String> by values {

    override fun toString(): String = values.joinToString()

    @JvmOverloads
    fun toString(separator: String, prefix: String = "", postfix: String = ""): String =
        values.joinToString(separator, prefix, postfix)
}
