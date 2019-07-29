/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.util.MemberReference
import com.google.common.collect.ImmutableBiMap
import java.nio.file.Files
import java.nio.file.Path

object TinySrgParser : SrgParser {
    private val commentRegex = Regex("#.+")

    override fun parseSrg(path: Path): McpSrgMap {
        val classMapBuilder = ImmutableBiMap.builder<String, String>()
        val fieldMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val methodMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val srgNames = hashMapOf<String, String>()

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

        return McpSrgMap(
            classMapBuilder.build(),
            fieldMapBuilder.build(),
            methodMapBuilder.build(),
            srgNames
        )
    }
}
