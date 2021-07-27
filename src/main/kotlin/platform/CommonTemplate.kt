/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.util.License
import com.intellij.openapi.project.Project
import java.time.ZonedDateTime

object CommonTemplate : BaseTemplate() {

    fun applyLicenseTemplate(
        project: Project,
        license: License,
        author: String
    ): String {
        val props = mapOf(
            "YEAR" to ZonedDateTime.now().year.toString(),
            "AUTHOR" to author
        )
        return project.applyTemplate("${license.id}.txt", props)
    }
}
