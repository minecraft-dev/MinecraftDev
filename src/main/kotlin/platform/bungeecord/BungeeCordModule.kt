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

package com.demonwav.mcdev.platform.bungeecord

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType
import com.demonwav.mcdev.platform.bukkit.PaperModuleType
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.application
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class BungeeCordModule<out T : AbstractModuleType<*>>(facet: MinecraftFacet, type: T) : AbstractModule(facet) {

    // Light test cases only support a single source content root, so we mix sources and resources under unit test mode
    private val pluginYmlSourceType = if (application.isUnitTestMode) SourceType.SOURCE else SourceType.RESOURCE

    var pluginYml by nullable {
        val file = facet.findFile("bungee.yml", pluginYmlSourceType)
        if (file != null) {
            return@nullable file
        }
        if (facet.isOfType(BukkitModuleType) || facet.isOfType(SpigotModuleType) || facet.isOfType(PaperModuleType)) {
            // If this module is _both_ a bungeecord and a bukkit module, then `plugin.yml` defaults to that platform
            // So we don't check
            return@nullable null
        }
        return@nullable facet.findFile("plugin.yml", pluginYmlSourceType)
    }
        private set

    override val type: PlatformType = type.platformType

    override val moduleType: T = type

    override val eventListenerGenSupport: EventListenerGenerationSupport? = BungeeCordEventListenerGenerationSupport()

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        BungeeCordConstants.EVENT_CLASS == eventClass.qualifiedName

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not a subclass of net.md_5.bungee.api.plugin.Event\n" +
            "Compiling and running this listener may result in a runtime exception"

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { (identifier.uastParent as? UClass)?.javaPsi }
            ?: return false

        val pluginInterface = JavaPsiFacade.getInstance(element.project)
            .findClass(BungeeCordConstants.PLUGIN, module.getModuleWithDependenciesAndLibrariesScope(false))
            ?: return false

        return !psiClass.hasModifier(JvmModifier.ABSTRACT) && psiClass.isInheritor(pluginInterface, true)
    }

    override fun dispose() {
        super.dispose()

        pluginYml = null
    }
}
