/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.VersionedConfig
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

object TranslationFunctionRepository : VersionedConfig<TranslationFunction>("translationFunctions", TranslationFunction::class.java) {
    override fun GsonBuilder.setup() {
        registerTypeAdapter(MemberReference::class.java, MemberReference.JsonAdapter)
    }

    override fun TranslationFunction.overrides(older: TranslationFunction): Boolean {
        return member == older.member
    }

    override fun getProjectModificationTracker(project: Project) =
        ServiceManager.getService(project, ProjectModificationTracking::class.java).tracker

    class ProjectModificationTracking {
        val tracker = ConfigModificationTracker()
    }
}
