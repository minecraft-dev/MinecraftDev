/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.util.text.VersionComparatorUtil

sealed class PluginUpdateStatus : Comparable<PluginUpdateStatus> {

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus {
        return maxOf(this, other)
    }

    protected abstract fun getVersionString(): String?

    override fun compareTo(other: PluginUpdateStatus): Int {
        val thisVersion = other.getVersionString() ?: return 1
        val otherVersion = other.getVersionString() ?: return -1
        return VersionComparatorUtil.compare(thisVersion, otherVersion)
    }

    object LatestVersionInstalled : PluginUpdateStatus() {
        override fun getVersionString(): String? = PluginUtil.pluginVersion
    }

    class Update(val pluginDescriptor: IdeaPluginDescriptor, val hostToInstallFrom: String?) : PluginUpdateStatus() {
        override fun getVersionString(): String? = this.pluginDescriptor.version
    }

    class CheckFailed(val message: String) : PluginUpdateStatus() {
        override fun getVersionString(): String? = null
    }
}
