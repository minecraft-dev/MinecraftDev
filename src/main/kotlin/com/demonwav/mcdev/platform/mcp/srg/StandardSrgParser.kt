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

object StandardSrgParser : SrgParser {
    override fun parseSrg(path: Path): McpSrgMap {
        val classMapBuilder = ImmutableBiMap.builder<String, String>()
        val fieldMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val methodMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
        val srgNames = hashMapOf<String, String>()

        Files.lines(path).forEach { line ->
            val parts = line.split(' ')
            when (parts[0]) {
                "CL:" -> {
                    val srg = parts[1].replace('/', '.')
                    val mcp = parts[2].replace('/', '.')
                    classMapBuilder.put(mcp, srg)
                }
                "FD:" -> {
                    val mcp = SrgMemberReference.parse(parts[1])
                    val srg = SrgMemberReference.parse(parts[2])
                    fieldMapBuilder.put(mcp, srg)
                    srgNames[srg.name] = mcp.name
                }
                "MD:" -> {
                    val mcp = SrgMemberReference.parse(parts[1], parts[2])
                    val srg = SrgMemberReference.parse(parts[3], parts[4])
                    methodMapBuilder.put(mcp, srg)
                    srgNames[srg.name] = mcp.name
                }
            }
        }

        return McpSrgMap(classMapBuilder.build(), fieldMapBuilder.build(), methodMapBuilder.build(), srgNames)
    }
}
