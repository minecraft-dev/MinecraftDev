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

package com.demonwav.mcdev.platform.minecraft.ct.inspections

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.minecraft.ct.gen.psi.CtHeader
import com.demonwav.mcdev.platform.minecraft.ct.gen.psi.CtVisitor
import com.demonwav.mcdev.platform.minecraft.ct.psi.mixins.CtHeaderMixin
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder

class UnknownCtNamespaceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : CtVisitor() {
        override fun visitHeaderMixin(header: CtHeaderMixin) {
            val namespace = header.namespaceString ?: return

            val module = header.findModule() ?: return
            val fabricModule = MinecraftFacet.getInstance(module, FabricModuleType) ?: return

            if (namespace !in fabricModule.mappingNamespaces) {
                holder.registerProblem(
                    (header as CtHeader).namespaceElement ?: header,
                    "Unrecognized namespace"
                )
            }
        }
    }
}
