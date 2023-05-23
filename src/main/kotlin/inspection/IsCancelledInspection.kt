/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
    override fun getDisplayName() = "Useless event isCancelled check"

    override fun getStaticDescription(): String = "Reports useless event cancellation checks"

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
