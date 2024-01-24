/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.forge.inspections.sideonly

data class SideAnnotation(
    val annotationName: String,
    val enumName: String,
    val clientValue: String,
    val serverValue: String,
) {
    val simpleEnumName: String = enumName.substringAfterLast('.')

    fun renderSide(side: Side): String? = when (side) {
        Side.CLIENT -> "$simpleEnumName.$clientValue"
        Side.SERVER -> "$simpleEnumName.$serverValue"
        else -> null
    }

    fun renderFQSide(side: Side): String? = when (side) {
        Side.CLIENT -> "$enumName.$clientValue"
        Side.SERVER -> "$enumName.$serverValue"
        else -> null
    }

    companion object {
        val KNOWN_ANNOTATIONS = listOf(
            SideAnnotation(
                "net.minecraftforge.fml.relauncher.SideOnly",
                "net.minecraftforge.fml.relauncher.Side",
                "CLIENT",
                "SERVER",
            ),
            SideAnnotation(
                "cpw.mods.fml.relauncher.SideOnly",
                "cpw.mods.fml.relauncher.Side",
                "CLIENT",
                "SERVER",
            ),
            SideAnnotation(
                "net.minecraftforge.api.distmarker.OnlyIn",
                "net.minecraftforge.api.distmarker.Dist",
                "CLIENT",
                "DEDICATED_SERVER",
            ),
            SideAnnotation(
                "net.neoforged.api.distmarker.OnlyIn",
                "net.neoforged.api.distmarker.Dist",
                "CLIENT",
                "DEDICATED_SERVER",
            ),
            SideAnnotation(
                "net.fabricmc.api.Environment",
                "net.fabricmc.api.EnvType",
                "CLIENT",
                "SERVER",
            ),
        )
    }
}
