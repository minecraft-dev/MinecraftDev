package com.demonwav.mcdev.creator.custom.model

@TemplateApi
data class ClassFqn(val fqn: String) {

    /**
     * The [Class.simpleName] of this class.
     */
    val className by lazy { fqn.substringAfterLast('.') }

    /**
     * The relative filesystem path to this class, without extension.
     */
    val path by lazy { fqn.replace('.', '/') }

    /**
     * The package name of this FQN as it would appear in source code.
     */
    val packageName by lazy { fqn.substringBeforeLast('.') }

    /**
     * The package path of this FQN reflected as a local filesystem path
     */
    val packagePath by lazy { packageName.replace('.', '/') }

    fun withClassName(className: String) = copy("$packageName.$className")

    override fun toString(): String = fqn
}
