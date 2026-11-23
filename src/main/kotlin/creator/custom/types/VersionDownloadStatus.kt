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

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.NoVersionsFoundException
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.and
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.AsyncProcessIcon

/**
 * Tracks a version property's download, for display with [downloadStatus].
 */
class VersionDownloadStatus(graph: PropertyGraph) {

    /**
     * True until the versions are downloaded. Stays true after a failure.
     */
    val loading: GraphProperty<Boolean> = graph.property(true)

    /**
     * The message shown for a failed download, or empty if it hasn't failed.
     */
    val failure: GraphProperty<String> = graph.property("")

    val failed: ObservableProperty<Boolean> = failure.transform { it.isNotEmpty() }

    fun succeeded() {
        failure.set("")
        loading.set(false)
    }

    fun failed(message: String = MCDevBundle("creator.ui.versions_download.failed")) {
        failure.set(message)
    }

    /**
     * Shows a failure message that includes the message of [cause].
     */
    fun failed(cause: Throwable) {
        failed(
            if (cause is NoVersionsFoundException) {
                MCDevBundle("creator.ui.versions_download.none_found")
            } else {
                MCDevBundle("creator.ui.versions_download.failed_cause", describe(cause))
            }
        )
    }

    /**
     * Shows a failure message that names the part that failed and includes the message of its cause.
     */
    fun failed(failure: VersionPartFailure) {
        val (part, cause) = failure
        failed(
            if (cause is NoVersionsFoundException) {
                MCDevBundle("creator.ui.versions_download.none_found_part", part)
            } else {
                MCDevBundle("creator.ui.versions_download.failed_part", part, describe(cause))
            }
        )
    }

    private fun describe(cause: Throwable): String = cause.message ?: cause.javaClass.simpleName
}

/**
 * A failed part of a property that downloads several sets of versions. [part] names the versions in the UI.
 */
data class VersionPartFailure(val part: String, val cause: Throwable)

/**
 * Downloads one part of a multi-part property unless [cached] is already set, passing a successful result to [store].
 * Returns the failure, or null if the part is available.
 */
internal inline fun <T : Any> downloadPart(
    part: String,
    cached: T?,
    store: (T) -> Unit,
    download: () -> Result<T>,
): VersionPartFailure? {
    if (cached != null) {
        return null
    }
    return download().fold(
        onSuccess = {
            store(it)
            null
        },
        onFailure = { VersionPartFailure(part, it) },
    )
}

/**
 * Adds a spinner to this row while [status] is loading, and the failure message once it fails. With [loadingLabel],
 * a "Downloading versions" label shows next to the spinner.
 */
fun Row.downloadStatus(status: VersionDownloadStatus, iconKey: String, loadingLabel: Boolean = false) {
    val inProgress = status.loading and !status.failed
    cell(AsyncProcessIcon(iconKey)).visibleIf(inProgress)
    if (loadingLabel) {
        label(MCDevBundle("creator.ui.versions_download.label")).visibleIf(inProgress)
    }
    label("").applyToComponent { foreground = JBColor.RED }
        .bindText(status.failure)
        .visibleIf(status.failed)
}
