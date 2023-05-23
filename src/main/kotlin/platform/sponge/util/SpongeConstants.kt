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

package com.demonwav.mcdev.platform.sponge.util

import com.demonwav.mcdev.util.SemanticVersion
import java.util.regex.Pattern

object SpongeConstants {

    const val PLUGIN_ANNOTATION = "org.spongepowered.api.plugin.Plugin"
    const val JVM_PLUGIN_ANNOTATION = "org.spongepowered.plugin.builtin.jvm.Plugin"
    const val DEPENDENCY_ANNOTATION = "org.spongepowered.api.plugin.Dependency"
    const val TEXT_COLORS = "org.spongepowered.api.text.format.TextColors"
    const val EVENT = "org.spongepowered.api.event.Event"
    const val LISTENER_ANNOTATION = "org.spongepowered.api.event.Listener"
    const val GETTER_ANNOTATION = "org.spongepowered.api.event.filter.Getter"
    const val IS_CANCELLED_ANNOTATION = "org.spongepowered.api.event.filter.IsCancelled"
    const val CANCELLABLE = "org.spongepowered.api.event.Cancellable"
    const val EVENT_ISCANCELLED_METHOD_NAME = "isCancelled"
    const val DEFAULT_CONFIG_ANNOTATION = "org.spongepowered.api.config.DefaultConfig"
    const val CONFIG_DIR_ANNOTATION = "org.spongepowered.api.config.ConfigDir"
    const val ASSET_ID_ANNOTATION = "org.spongepowered.api.asset.AssetId"
    const val INJECT_ANNOTATION = "com.google.inject.Inject"

    // Taken from https://github.com/SpongePowered/plugin-meta/blob/185f5c2/src/main/java/org/spongepowered/plugin/meta/PluginMetadata.java#L60
    private const val ID_PATTERN_STRING = "^[a-z][a-z0-9-_]{1,63}$"
    val ID_PATTERN: Pattern = Pattern.compile(ID_PATTERN_STRING)

    val API8 = SemanticVersion.release(8)
    val API9 = SemanticVersion.release(9)
}
