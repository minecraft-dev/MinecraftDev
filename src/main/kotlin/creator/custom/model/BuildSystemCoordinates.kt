package com.demonwav.mcdev.creator.custom.model

@TemplateApi
data class BuildSystemCoordinates(val groupId: String, val artifactId: String, val version: String) {

    override fun toString(): String = "$groupId:$artifactId:$version"
}
