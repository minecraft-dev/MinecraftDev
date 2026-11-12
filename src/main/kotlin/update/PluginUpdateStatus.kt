/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.util.text.VersionComparatorUtil

sealed class PluginUpdateStatus : Comparable<PluginUpdateStatus> {

    protected abstract fun getVersionString(): String?

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus = maxOf(this, other)

    override fun compareTo(other: PluginUpdateStatus): Int {
        val thisVersion = this.getVersionString() ?: return 1
        val otherVersion = other.getVersionString() ?: return -1
        return VersionComparatorUtil.compare(thisVersion, otherVersion)
    }

    object LatestVersionInstalled : PluginUpdateStatus() {
        override fun getVersionString(): String = PluginUtil.pluginVersion
    }

    class Update(val pluginDescriptor: IdeaPluginDescriptor, val hostToInstallFrom: String?) : PluginUpdateStatus() {
        override fun getVersionString(): String? = this.pluginDescriptor.version
    }

    class CheckFailed(val message: String) : PluginUpdateStatus() {
        override fun getVersionString(): String? = null
    }
}
