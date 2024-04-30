package com.demonwav.mcdev.creator.custom.model

data class ClassFqn(val fqn: String) {

    val className by lazy { fqn.substringAfterLast('.') }
    val path by lazy { fqn.replace('.', '/') }
    val packageName by lazy { fqn.substringBeforeLast('.') }
    val packagePath by lazy { packageName.replace('.', '/') }

    override fun toString(): String = fqn
}
