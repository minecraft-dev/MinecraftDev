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
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.lang.reflect.Type

object TranslationFunctionRepository : VersionedConfig<TranslationFunction>("translationFunctions", TranslationFunction::class.java) {
    override fun GsonBuilder.setup() {
        registerTypeAdapter(MemberReference::class.java, MemberReferenceDeserializer)
    }

    override fun TranslationFunction.overrides(older: TranslationFunction): Boolean {
        return member == older.member
    }

    override fun getProjectModificationTracker(project: Project) =
        ServiceManager.getService(project, ProjectModificationTracking::class.java).tracker

    object MemberReferenceDeserializer : JsonDeserializer<MemberReference> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): MemberReference {
            val ref = json.asString
            val className = ref.substringBefore('#')
            val methodName = ref.substring(className.length + 1, ref.indexOf("("))
            val methodDesc = ref.substring(className.length + methodName.length + 1)
            return MemberReference(methodName, methodDesc, className)
        }
    }

    class ProjectModificationTracking {
        val tracker = ConfigModificationTracker()
    }
}
