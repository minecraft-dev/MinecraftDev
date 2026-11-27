/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.index

import com.demonwav.mcdev.platform.minecraft.unpick.UnpickFile
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickFileType
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

class UnpickGroupIndex : ScalarIndexExtension<String>() {
    override fun getName() = Util.NAME

    override fun getVersion() = 2

    override fun dependsOnFileContent() = true

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> = Indexer

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(UnpickFileType)

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    private object Indexer : DataIndexer<String, Void?, FileContent> {
        override fun map(inputData: FileContent): Map<String, Void?> {
            val file = inputData.psiFile as? UnpickFile ?: return emptyMap()
            val keys = HashMap<String, Void?>()
            for (definition in file.groupDefinitions) {
                definition.name?.let { keys[Util.nameKey(it)] = null }
                for (key in Util.conflictKeys(definition)) {
                    keys[key] = null
                }
            }
            return keys
        }
    }

    object Util {
        val NAME = ID.create<String, Void?>("mcdev.UnpickGroupIndex")

        private const val NAME_PREFIX = "n:"
        private const val CONFLICT_PREFIX = "c:"
        private val WHITESPACE = Regex("\\s+")

        /**
         * The key of files that define the named group.
         */
        fun nameKey(name: String) = NAME_PREFIX + name

        /**
         * Returns the group name of a key from [nameKey], or `null` for any other key.
         */
        fun nameFromKey(key: String): String? = key.takeIf { it.startsWith(NAME_PREFIX) }?.removePrefix(NAME_PREFIX)

        /**
         * Returns the keys of [definition] for finding constants it can conflict with. Two definitions share a key when
         * they belong to the same group, have the same type, and are both unscoped or have a scope in common.
         *
         * Returns an empty list if the definition doesn't have a valid group type.
         */
        fun conflictKeys(definition: UnpickGroupDefinition): List<String> {
            val type = definition.dataType?.dataType?.takeIf { it.isValidGroupType } ?: return emptyList()
            val prefix = "$CONFLICT_PREFIX${definition.name.orEmpty()}:${type.keyword}:"
            val scopes = definition.scopes
            if (scopes.isEmpty()) {
                return listOf(prefix)
            }
            return scopes.map { prefix + it.text.split(WHITESPACE).joinToString(" ") }.distinct()
        }
    }
}
