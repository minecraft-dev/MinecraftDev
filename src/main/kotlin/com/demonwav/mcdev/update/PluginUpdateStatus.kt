/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.util.text.VersionComparatorUtil

sealed class PluginUpdateStatus {

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus {
        // Jesus wept. https://github.com/JetBrains/kotlin/blob/master/idea/src/org/jetbrains/kotlin/idea/KotlinPluginUpdater.kt#L61-L63
        if (other is Update && (
            this is LatestVersionInstalled || this is Update && VersionComparatorUtil.compare(
                other.pluginDescriptor.version,
                this.pluginDescriptor.version
            ) > 0)
        ) {
            return other
        }
        return this
    }

    class LatestVersionInstalled : PluginUpdateStatus()
    class Update(val pluginDescriptor: IdeaPluginDescriptor, val hostToInstallFrom: String?) : PluginUpdateStatus()
    class CheckFailed(val message: String) : PluginUpdateStatus()
}
