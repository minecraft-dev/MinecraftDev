package com.demonwav.mcdev.platform.sponge.util

import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.projectRoots.JavaSdkVersion

object SpongeVersions {

    val API8 = SemanticVersion.parse("8.0.0") to JavaSdkVersion.JDK_16
    val API9 = SemanticVersion.parse("9.0.0") to JavaSdkVersion.JDK_17
    val API10 = SemanticVersion.parse("10.0.0") to JavaSdkVersion.JDK_17
    val API11 = SemanticVersion.parse("11.0.0") to JavaSdkVersion.JDK_21

    fun requiredJavaVersion(spongeApiVersion: SemanticVersion): JavaSdkVersion {
        for ((api, java) in listOf(API8, API9, API10, API11).reversed()) {
            if (spongeApiVersion >= api) {
                return java
            }
        }

        return JavaSdkVersion.JDK_17
    }
}
