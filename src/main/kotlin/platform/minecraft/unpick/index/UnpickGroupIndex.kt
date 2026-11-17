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
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

/**
 * Indexes the names of the groups defined in each unpick file. The default group is indexed under the empty string.
 */
class UnpickGroupIndex : ScalarIndexExtension<String>() {
    override fun getName() = NAME

    override fun getVersion() = 1

    override fun dependsOnFileContent() = true

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> = Indexer

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(UnpickFileType)

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    private object Indexer : DataIndexer<String, Void?, FileContent> {
        override fun map(inputData: FileContent): Map<String, Void?> {
            val file = inputData.psiFile as? UnpickFile ?: return emptyMap()
            return file.groupDefinitions.associate { (it.name ?: DEFAULT_GROUP_KEY) to null }
        }
    }

    companion object {
        val NAME = ID.create<String, Void?>("mcdev.UnpickGroupIndex")

        const val DEFAULT_GROUP_KEY = ""
    }
}
