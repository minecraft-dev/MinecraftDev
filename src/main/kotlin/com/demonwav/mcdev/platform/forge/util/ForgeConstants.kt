/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.util

object ForgeConstants {

    const val SIDE_ONLY_ANNOTATION = "net.minecraftforge.fml.relauncher.SideOnly"
    const val SIDE_ANNOTATION = "net.minecraftforge.fml.relauncher.Side"
    const val SIDED_PROXY_ANNOTATION = "net.minecraftforge.fml.common.SidedProxy"
    const val MOD_ANNOTATION = "net.minecraftforge.fml.common.Mod"
    const val CORE_MOD_INTERFACE = "net.minecraftforge.fml.relauncher.IFMLLoadingPlugin"
    const val EVENT_HANDLER_ANNOTATION = "net.minecraftforge.fml.common.Mod.EventHandler"
    const val SUBSCRIBE_EVENT_ANNOTATION = "net.minecraftforge.fml.common.eventhandler.SubscribeEvent"
    const val FML_EVENT = "net.minecraftforge.fml.common.event.FMLEvent"
    const val EVENT = "net.minecraftforge.fml.common.eventhandler.Event"
    const val NETWORK_MESSAGE = "net.minecraftforge.fml.common.network.simpleimpl.IMessage"
    const val NETWORK_MESSAGE_HANDLER = "net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler"
    const val MCMOD_INFO = "mcmod.info"
    const val META_INF = "META-INF"
    const val MODS_TOML = "mods.toml"
    const val PACK_MCMETA = "pack.mcmeta"
}
