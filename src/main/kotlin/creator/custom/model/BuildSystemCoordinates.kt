package com.demonwav.mcdev.creator.custom.model

data class BuildSystemCoordinates(val groupId: String, val artifactId: String, val version: String) {

    override fun toString(): String = "$groupId:$artifactId:$version"
}
