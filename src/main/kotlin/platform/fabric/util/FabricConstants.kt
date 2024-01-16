/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.util

object FabricConstants {

    const val FABRIC_MOD_JSON = "fabric.mod.json"

    const val MOD_INITIALIZER = "net.fabricmc.api.ModInitializer"
    const val CLIENT_MOD_INITIALIZER = "net.fabricmc.api.ClientModInitializer"
    const val SERVER_MOD_INITIALIZER = "net.fabricmc.api.DedicatedServerModInitializer"
    const val PRE_LAUNCH_ENTRYPOINT = "net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint"
    const val ENVIRONMENT_ANNOTATION = "net.fabricmc.api.Environment"
    const val ENV_TYPE = "net.fabricmc.api.EnvType"
    const val ENVIRONMENT_INTERFACE_ANNOTATION = "net.fabricmc.api.EnvironmentInterface"

    val ENTRYPOINTS = setOf(MOD_INITIALIZER, CLIENT_MOD_INITIALIZER, SERVER_MOD_INITIALIZER, PRE_LAUNCH_ENTRYPOINT)
    val ENTRYPOINT_BY_TYPE = mapOf(
        "main" to MOD_INITIALIZER,
        "client" to CLIENT_MOD_INITIALIZER,
        "server" to SERVER_MOD_INITIALIZER,
        "preLaunch" to PRE_LAUNCH_ENTRYPOINT
    )
}
