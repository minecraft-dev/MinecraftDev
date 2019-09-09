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

sealed class PluginUpdateStatus {

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus {
        if (other is Update) {
            when (this) {
                is LatestVersionInstalled -> return other
                is Update -> {
                    val otherVersion = other.pluginDescriptor.version
                    val thisVersion = this.pluginDescriptor.version
                    if (VersionComparatorUtil.compare(otherVersion, thisVersion) > 0) {
                        return other
                    }
                }
            }
        }
        return this
    }

    object LatestVersionInstalled : PluginUpdateStatus()
    class Update(val pluginDescriptor: IdeaPluginDescriptor, val hostToInstallFrom: String?) : PluginUpdateStatus()
    class CheckFailed(val message: String) : PluginUpdateStatus()
}
