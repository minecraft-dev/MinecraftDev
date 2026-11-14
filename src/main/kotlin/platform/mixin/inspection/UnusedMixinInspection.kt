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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.platform.forge.inspections.sideonly.SideOnlyUtil
import com.demonwav.mcdev.platform.mixin.MixinModule
import com.demonwav.mcdev.platform.mixin.config.MixinConfig
import com.demonwav.mcdev.platform.mixin.util.findStubClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.createSmartPointer
import com.intellij.psi.search.GlobalSearchScope

class UnusedMixinInspection : MixinInspection() {

    override fun buildVisitor(holder: ProblemsHolder) = Visitor(holder)

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitClass(clazz: PsiClass) {
            val module = clazz.findModule() ?: return
            if (clazz.isMixin) {
                for (config in MixinModule.getMixinConfigs(module.project, GlobalSearchScope.moduleScope(module))) {
                    if (config.qualifiedMixins.any { it == clazz.fullQualifiedName }) {
                        return
                    }
                    if (config.qualifiedClient.any { it == clazz.fullQualifiedName }) {
                        return
                    }
                    if (config.qualifiedServer.any { it == clazz.fullQualifiedName }) {
                        return
                    }
                }

                val bestQuickFixConfig = MixinModule.getBestWritableConfigForMixinClass(
                    module.project,
                    GlobalSearchScope.moduleScope(module),
                    clazz.fullQualifiedName ?: "",
                )
                val problematicElement = clazz.nameIdentifier
                if (problematicElement != null) {
                    val bestQuickFixFile = bestQuickFixConfig?.file
                    val qualifiedName = clazz.fullQualifiedName
                    if (bestQuickFixFile != null && qualifiedName != null) {
                        var side = SideOnlyUtil.getSideForClass(clazz).second
                        if (side == Side.NONE || side == Side.INVALID) {
                            side = clazz.mixinTargets.mapFirstNotNull {
                                val stubClass = it.findStubClass(module.project) ?: return@mapFirstNotNull null
                                SideOnlyUtil.getSideForClass(stubClass)
                            }?.second ?: Side.NONE
                        }
                        val jsonQuickFixFile = PsiManager.getInstance(holder.project).findFile(bestQuickFixFile) as? JsonFile ?: return
                        val quickFix = QuickFix(jsonQuickFixFile.createSmartPointer(), qualifiedName, side)
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config", quickFix)
                    } else {
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config")
                    }
                }
            }
        }
    }

    private class QuickFix(
        private val quickFixFile: SmartPsiElementPointer<JsonFile>,
        private val qualifiedName: String,
        private val side: Side,
    ) : LocalQuickFix {

        private val sideDisplayName = when (side) {
            Side.CLIENT -> "client"
            Side.SERVER -> "server"
            else -> "common"
        }

        override fun getName() = "Add to Mixin config ($sideDisplayName side)"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val psiFile = quickFixFile.element ?: return
            val root = psiFile.topLevelValue as? JsonObject ?: return
            val config = MixinConfig(project, root)
            val mixinList = when (side) {
                Side.CLIENT -> config.qualifiedClient
                Side.SERVER -> config.qualifiedServer
                else -> config.qualifiedMixins
            }
            mixinList.add(qualifiedName)
        }

        override fun getFileModifierForPreview(target: PsiFile): FileModifier? {
            val file = quickFixFile.element ?: return null
            val root = file.topLevelValue as? JsonObject ?: return null
            @Suppress("StatefulEp")
            return object : LocalQuickFixOnPsiElement(root) {
                override fun getText(): @IntentionName String {
                    return "Add to Mixin config ($sideDisplayName side)"
                }
                override fun getFamilyName(): @IntentionFamilyName String = text

                override fun invoke(
                    project: Project,
                    psiFile: PsiFile,
                    startElement: PsiElement,
                    endElement: PsiElement
                ) {
                    val jsonObject = startElement as JsonObject
                    val config = MixinConfig(project, jsonObject)
                    val mixinList = when (side) {
                        Side.CLIENT -> config.qualifiedClient
                        Side.SERVER -> config.qualifiedServer
                        else -> config.qualifiedMixins
                    }
                    mixinList.add(qualifiedName)
                }
            }
        }

        override fun getFamilyName() = name
    }
}
