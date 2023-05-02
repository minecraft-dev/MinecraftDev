/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
