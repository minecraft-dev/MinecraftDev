package com.demonwav.mcdev.creator.custom.model

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk

@TemplateApi
data class CreatorJdk(val sdk: Sdk?) {

    val javaVersion: Int
        get() = sdk?.let { JavaSdk.getInstance().getVersion(it)?.ordinal } ?: 17
}
