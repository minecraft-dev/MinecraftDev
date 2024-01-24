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

package com.demonwav.mcdev.platform.neoforge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.neoforge.util.NeoForgeConstants
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.createVoidMethodWithParameterType
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.demonwav.mcdev.util.runWriteTaskLater
import com.demonwav.mcdev.util.waitForAllSmart
import com.intellij.json.JsonFileType
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class NeoForgeModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var mcmod by nullable { facet.findFile(NeoForgeConstants.MCMOD_INFO, SourceType.RESOURCE) }
        private set

    override val moduleType = NeoForgeModuleType
    override val type = PlatformType.NEOFORGE
    override val icon = PlatformAssets.NEOFORGE_ICON

    override fun init() {
        ApplicationManager.getApplication().executeOnPooledThread {
            waitForAllSmart()
            // Set mcmod.info icon
            runWriteTaskLater {
                FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, NeoForgeConstants.MCMOD_INFO)
                FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, NeoForgeConstants.PACK_MCMETA)
            }
        }
    }

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean {
        if (method == null) {
            return NeoForgeConstants.FML_EVENT == eventClass.qualifiedName ||
                NeoForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName
        }

        if (method.hasAnnotation(NeoForgeConstants.SUBSCRIBE_EVENT)) {
            return NeoForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName
        }

        // just default to true
        return true
    }

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod): String {
        return formatWrongEventMessage(
            NeoForgeConstants.EVENTBUS_EVENT,
            NeoForgeConstants.SUBSCRIBE_EVENT,
            NeoForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName,
        )
    }

    private fun formatWrongEventMessage(expected: String, suggested: String, wrong: Boolean): String {
        val base = "Parameter is not a subclass of $expected\n"
        if (wrong) {
            return base + "This method should be annotated with $suggested"
        }
        return base + "Compiling and running this listener may result in a runtime exception"
    }

    override fun isStaticListenerSupported(method: PsiMethod) = true

    override fun generateEventListenerMethod(
        containingClass: PsiClass,
        chosenClass: PsiClass,
        chosenName: String,
        data: GenerationData?,
    ): PsiMethod? {
        val method = createVoidMethodWithParameterType(project, chosenName, chosenClass) ?: return null
        val modifierList = method.modifierList

        modifierList.addAnnotation(NeoForgeConstants.SUBSCRIBE_EVENT)

        return method
    }

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { identifier.uastParent as? UClass }
            ?: return false

        return !psiClass.hasModifier(JvmModifier.ABSTRACT) &&
            psiClass.findAnnotation(NeoForgeConstants.MOD_ANNOTATION) != null
    }

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? = null

    override fun dispose() {
        mcmod = null
        super.dispose()
    }
}
