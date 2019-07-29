/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiMethodCallExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class IsCancelledInspection : BaseInspection() {
    @Nls
    override fun getDisplayName() = "Useless event is cancelled check"

    override fun buildErrorString(vararg infos: Any): String {
        val useless = infos[0] as IsCancelled
        return useless.errorString
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val useless = infos[0] as? IsCancelled
        return useless?.buildFix
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                val module = ModuleUtilCore.findModuleForPsiElement(expression) ?: return

                val instance = MinecraftFacet.getInstance(module) ?: return

                val useless = instance.modules.mapFirstNotNull { m -> m.checkUselessCancelCheck(expression) } ?: return

                registerMethodCallError(expression, useless)
            }
        }
    }
}
