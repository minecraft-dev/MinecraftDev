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

package com.demonwav.mcdev.platform.fabric.annotater

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object AnnotaterNotifications {
    const val GROUP_ID = "Minecraft annotater"

    fun describe(status: AnnotaterCheckoutService.Status): String = when (status) {
        is AnnotaterCheckoutService.Status.Available -> status.checkout.root.toString()
        AnnotaterCheckoutService.Status.Disabled -> MCDevBundle("fabric.annotater.status.disabled")
        AnnotaterCheckoutService.Status.UnknownMinecraftVersion ->
            MCDevBundle("fabric.annotater.status.unknown_version")

        is AnnotaterCheckoutService.Status.NotCloned -> if (AnnotaterVcs.instance != null) {
            MCDevBundle("fabric.annotater.status.not_cloned", status.version)
        } else {
            MCDevBundle("fabric.annotater.status.not_cloned_no_git", status.version)
        }

        is AnnotaterCheckoutService.Status.VersionMismatch ->
            MCDevBundle("fabric.annotater.status.version_mismatch", status.actual, status.expected)

        is AnnotaterCheckoutService.Status.Invalid -> MCDevBundle("fabric.annotater.status.invalid", status.message)
    }

    fun notifyUnavailable(project: Project, status: AnnotaterCheckoutService.Status) {
        val notification = Notification(
            GROUP_ID,
            MCDevBundle("fabric.annotater.unavailable"),
            describe(status),
            NotificationType.WARNING,
        )
        val vcs = AnnotaterVcs.instance
        if (status is AnnotaterCheckoutService.Status.NotCloned && vcs != null) {
            notification.addAction(
                NotificationAction.createSimpleExpiring(MCDevBundle("fabric.annotater.clone", status.version)) {
                    vcs.cloneOrUpdate(project, status.version)
                },
            )
        }
        notification.addAction(
            NotificationAction.createSimpleExpiring(MCDevBundle("fabric.annotater.configure")) {
                AnnotaterConfigurable.show(project)
            },
        )
        notification.notify(project)
    }

    fun notifyError(project: Project?, title: String, error: Throwable) {
        Notification(GROUP_ID, title, error.message ?: error.toString(), NotificationType.ERROR).notify(project)
    }

    fun notifyInfo(project: Project?, title: String, content: String) {
        Notification(GROUP_ID, title, content, NotificationType.INFORMATION).notify(project)
    }
}
