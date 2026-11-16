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

import com.intellij.util.messages.Topic
import java.nio.file.Path

interface AnnotaterListener {
    /**
     * Called when annotater settings change, or a checkout is created, updated or switches branch.
     */
    fun checkoutsChanged() {}

    /**
     * Called when the Javadocs of [outerClass] (an internal name) in the checkout at [checkoutRoot] change.
     */
    fun javadocsChanged(checkoutRoot: Path, outerClass: String) {}

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(AnnotaterListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
