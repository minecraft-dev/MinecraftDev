/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitModule
import com.demonwav.mcdev.platform.bungeecord.generation.BungeeCordGenerationData
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.nullable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil

class BungeeCordModule<out T : AbstractModuleType<*>>(facet: MinecraftFacet, override val moduleType: T) :
    AbstractModule(facet) {

    var pluginYml by nullable { facet.findFile("plugin.yml", SourceType.RESOURCE) }
        private set

    override val type = PlatformType.BUNGEECORD
    override val icon = PlatformAssets.BUNGEECORD_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        BungeeCordConstants.EVENT_CLASS == eventClass.qualifiedName

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not a subclass of net.md_5.bungee.api.plugin.Event\n" +
            "Compiling and running this listener may result in a runtime exception"

    override fun doPreEventGenerate(psiClass: PsiClass, data: GenerationData?) {
        val bungeeCordListenerClass = BungeeCordConstants.LISTENER_CLASS

        if (!psiClass.extendsOrImplements(bungeeCordListenerClass)) {
            psiClass.addImplements(bungeeCordListenerClass)
        }
    }

    override fun generateEventListenerMethod(
        containingClass: PsiClass,
        chosenClass: PsiClass,
        chosenName: String,
        data: GenerationData?
    ): PsiMethod? {
        val method = BukkitModule.generateBukkitStyleEventListenerMethod(
            chosenClass,
            chosenName,
            project,
            BungeeCordConstants.HANDLER_ANNOTATION,
            false
        ) ?: return null

        val generationData = data as BungeeCordGenerationData? ?: return method

        val modifierList = method.modifierList
        val annotation = modifierList.findAnnotation(BungeeCordConstants.HANDLER_ANNOTATION) ?: return method

        if (generationData.eventPriority == "NORMAL") {
            return method
        }

        val value = JavaPsiFacade.getElementFactory(project)
            .createExpressionFromText(
                BungeeCordConstants.EVENT_PRIORITY_CLASS + "." + generationData.eventPriority,
                annotation
            )

        annotation.setDeclaredAttributeValue("priority", value)

        return method
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        if (element.parent !is PsiClass) {
            return false
        }

        val project = element.project
        val psiClass = element.parent as PsiClass
        val pluginClass = JavaPsiFacade.getInstance(project)
            .findClass(BungeeCordConstants.PLUGIN, GlobalSearchScope.allScope(project))

        return pluginClass != null && psiClass.extendsListTypes.any { c -> c == PsiTypesUtil.getClassType(pluginClass) }
    }

    override fun dispose() {
        super.dispose()

        pluginYml = null
    }
}
