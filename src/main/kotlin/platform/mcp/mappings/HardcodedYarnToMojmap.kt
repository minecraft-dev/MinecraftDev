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

package com.demonwav.mcdev.platform.mcp.mappings

import com.demonwav.mcdev.util.MemberReference
import com.google.common.collect.ImmutableBiMap

/**
 * A temporary solution until we get something more dynamic sorted
 */
object HardcodedYarnToMojmap {
    fun createMappings() = Mappings(
        ImmutableBiMap.ofEntries(
            "net.minecraft.item.ItemStack" mapTo "net.minecraft.world.item.ItemStack",
            "net.minecraft.util.Formatting" mapTo "net.minecraft.ChatFormatting",
            "net.minecraft.text.Text" mapTo "net.minecraft.network.chat.Component",
        ),
        ImmutableBiMap.ofEntries(),
        ImmutableBiMap.ofEntries(
            MemberReference(
                owner = "net.minecraft.util.Text",
                name = "stringifiedTranslatable",
                descriptor = "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"
            ) mapTo MemberReference(
                owner = "net.minecraft.network.chat.Component",
                name = "translatableEscape",
                descriptor = "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            MemberReference(
                owner = "net.minecraft.client.resource.language.I18n",
                name = "translate",
                descriptor = "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"
            ) mapTo MemberReference(
                owner = "net.minecraft.client.resources.language.I18n",
                name = "get",
                descriptor = "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"
            )
        ),
        hashMapOf(),
        false,
    )

    private infix fun <T> T.mapTo(value: T) = java.util.AbstractMap.SimpleEntry(this, value)
}
