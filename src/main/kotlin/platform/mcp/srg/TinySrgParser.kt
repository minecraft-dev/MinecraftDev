/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.util.MemberReference
import com.google.common.collect.ImmutableBiMap
import com.intellij.openapi.util.registry.Registry
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor

object TinySrgParser : SrgParser {
    private val commentRegex = Regex("#.+")

    override fun parseSrg(path: Path): McpSrgMap {
        val classMapBuilder = ImmutableBiMap.builder<String, String>()
        val fieldMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val methodMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val srgNames = hashMapOf<String, String>()

        if (Registry.`is`("mcdev.new.tsrg.parser")) {
            newParser(path, classMapBuilder, fieldMapBuilder, methodMapBuilder, srgNames)
        } else {
            oldParser(path, classMapBuilder, fieldMapBuilder, methodMapBuilder, srgNames)
        }

        return McpSrgMap(
            classMapBuilder.build(),
            fieldMapBuilder.build(),
            methodMapBuilder.build(),
            srgNames
        )
    }

    private fun oldParser(
        path: Path,
        classMapBuilder: ImmutableBiMap.Builder<String, String>,
        fieldMapBuilder: ImmutableBiMap.Builder<MemberReference, MemberReference>,
        methodMapBuilder: ImmutableBiMap.Builder<MemberReference, MemberReference>,
        srgNames: HashMap<String, String>
    ) {
        var classRef: Pair<String, String>? = null

        Files.lines(path).forEach { line ->
            val cleaned = commentRegex.replace(line, "")
            if (cleaned.isEmpty()) {
                return@forEach
            }

            if (!cleaned.startsWith('\t')) {
                val (mcp, srg) = cleaned.split(' ')
                val mcpRef = mcp.replace('/', '.')
                val srgRef = srg.replace('/', '.')
                classMapBuilder.put(mcpRef, srgRef)
                classRef = Pair(mcpRef, srgRef)
                return@forEach
            }

            // Don't error on an invalid file, just skip
            val clazz = classRef ?: return@forEach

            val parts = cleaned.split(' ')
            if (parts.size == 2) {
                // Field
                val (mcp, srg) = cleaned.substring(1).split(' ')
                val mcpRef = SrgMemberReference.parse(clazz.first + "/" + mcp)
                val srgRef = SrgMemberReference.parse(clazz.second + "/" + srg)
                fieldMapBuilder.put(mcpRef, srgRef)
                srgNames[srg] = mcp
            } else if (parts.size == 3) {
                // Method
                val (mcp, sig, srg) = cleaned.substring(1).split(' ')
                val mcpRef = SrgMemberReference.parse(clazz.first + "/" + mcp, sig)
                val srgRef = SrgMemberReference.parse(clazz.second + "/" + srg, sig)
                methodMapBuilder.put(mcpRef, srgRef)
                srgNames[srg] = mcp
            }
        }
    }

    private fun newParser(
        path: Path,
        classMapBuilder: ImmutableBiMap.Builder<String, String>,
        fieldMapBuilder: ImmutableBiMap.Builder<MemberReference, MemberReference>,
        methodMapBuilder: ImmutableBiMap.Builder<MemberReference, MemberReference>,
        srgNames: HashMap<String, String>
    ) {
        val visitor = object : MappingVisitor {
            var namedNsIndex = -1

            lateinit var cls: String
            var desc: String? = null
            var inter: String? = null
            var named: String? = null

            override fun visitNamespaces(srcNamespace: String, dstNamespaces: MutableList<String>) {
                namedNsIndex = dstNamespaces.indexOf("right")
            }

            override fun visitContent(): Boolean {
                return namedNsIndex >= 0
            }

            override fun visitClass(srcName: String): Boolean {
                cls = srcName.replace('/', '.')
                desc = null
                inter = null
                named = null
                return true
            }

            override fun visitField(srcName: String, srcDesc: String?): Boolean {
                inter = srcName
                return true
            }

            override fun visitMethod(srcName: String, srcDesc: String): Boolean {
                inter = srcName
                desc = srcDesc
                return true
            }

            override fun visitMethodArg(argPosition: Int, lvIndex: Int, srcName: String): Boolean {
                return false
            }

            override fun visitMethodVar(lvtRowIndex: Int, lvIndex: Int, startOpIdx: Int, srcName: String): Boolean {
                return false
            }

            override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String) {
                when (namespace) {
                    namedNsIndex -> named = name
                    else -> return
                }

                if (targetKind == MappedElementKind.CLASS && named != null) {
                    classMapBuilder.put(cls, named!!.replace('/', '.'))
                } else if (inter != null && named != null) {
                    if (targetKind == MappedElementKind.FIELD) {
                        fieldMapBuilder.put(MemberReference(inter!!, null, cls), MemberReference(named!!, null, cls))
                        srgNames[inter!!] = named!!
                    } else if (targetKind == MappedElementKind.METHOD) {
                        methodMapBuilder.put(MemberReference(inter!!, desc, cls), MemberReference(named!!, desc, cls))
                        srgNames[inter!!] = named!!
                    }
                }
            }

            override fun visitComment(targetKind: MappedElementKind, comment: String) {
            }

            override fun visitElementContent(targetKind: MappedElementKind?): Boolean {
                return targetKind != MappedElementKind.METHOD
            }
        }

        MappingReader.read(path, visitor)
    }
}
