/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.color.McpColorMethod.FloatVectorParam
import com.demonwav.mcdev.platform.mcp.color.McpColorMethod.IntVectorParam
import com.demonwav.mcdev.platform.mcp.color.McpColorMethod.Param
import com.demonwav.mcdev.platform.mcp.color.McpColorMethod.SingleIntParam
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.SemanticVersion
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.util.io.inputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object McpColorMethods {
    private val entries by lazy {
        val result = load()
        result.mapValues { ref ->
            result.entries.filter { it.key <= ref.key }
                .sortedBy { it.key }
                .map { it.value }
                .reduce { acc, cur ->
                    acc.filter { a -> cur.none { b -> a.member == b.member } } + cur
                }
        }
    }

    operator fun get(elem: PsiElement): List<McpColorMethod> {
        val module = ModuleUtilCore.findModuleForPsiElement(elem) ?: return emptyList()
        val facet = MinecraftFacet.getInstance(module) ?: return emptyList()
        return facet.getModuleOfType(McpModuleType)?.colorMethods ?: emptyList()
    }

    operator fun get(mcVersion: String): List<McpColorMethod> {
        val semVer = SemanticVersion.parse(mcVersion)
        return entries.entries.findLast { it.key <= semVer }?.value ?: emptyList()
    }

    private fun load(): Map<SemanticVersion, List<McpColorMethod>> {
        val url = javaClass.getResource("/configs/mcp/colors")
        val files = url.toURI().listFiles()
        return files
            .filter { it.fileName.toString().endsWith(".json") }
            .associate {
                val version = SemanticVersion.parse(it.fileName.toString().substringBeforeLast('.'))
                version to load(it.inputStream())
            }
    }

    private fun URI.listFiles(): List<Path> {
        val parts = this.toString().split("!", limit = 2)
        val path = when (parts.size) {
            1 -> Paths.get(this)
            else -> {
                val env = mutableMapOf<String, String>()
                FileSystems.newFileSystem(URI.create(parts[0]), env).getPath(parts[1])
            }
        }
        return Files.list(path).toList()
    }

    private fun load(stream: InputStream): List<McpColorMethod> {
        val content = InputStreamReader(stream)
        val gson = GsonBuilder()
            .registerTypeAdapter(Param::class.java, McpMethodParamDeserializer)
            .registerTypeAdapter(MemberReference::class.java, MemberReferenceDeserializer)
            .create()
        return gson.fromJson(content, McpColorFile::class.java).entries
    }

    class McpColorFile(val entries: List<McpColorMethod>)

    object McpMethodParamDeserializer : JsonDeserializer<Param> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): Param {
            val obj = json.asJsonObject
            val discriminator = obj.get("type").asString
            return when (discriminator) {
                "intvec" -> IntVectorParam.Deserializer.deserialize(json, type, ctx)
                "floatvec" -> FloatVectorParam.Deserializer.deserialize(json, type, ctx)
                else -> SingleIntParam.Deserializer.deserialize(json, type, ctx)
            }
        }
    }

    object MemberReferenceDeserializer : JsonDeserializer<MemberReference> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): MemberReference {
            val ref = json.asString
            val className = ref.substringBefore('#')
            val methodName = ref.substring(className.length + 1, ref.indexOf("("))
            val methodDesc = ref.substring(className.length + methodName.length + 1)
            return MemberReference(methodName, methodDesc, className)
        }
    }
}
