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

package com.demonwav.mcdev.platform.mcp.inspections

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiTypeParameter
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import org.jetbrains.annotations.Nls

class EntityConstructorInspection : BaseInspection() {
    @Nls
    override fun getDisplayName(): String {
        return "MCP Entity class missing World constructor"
    }

    override fun buildErrorString(vararg infos: Any): String {
        return "All entities must have a constructor that takes one " + McpConstants.WORLD + " parameter."
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (aClass is PsiTypeParameter) {
                    return
                }

                if (!aClass.extendsOrImplements(McpConstants.ENTITY)) {
                    return
                }

                if (aClass.extendsOrImplements(McpConstants.ENTITY_FX)) {
                    return
                }

                val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return
                val mcpModule = MinecraftFacet.getInstance(module, McpModuleType) ?: return
                val mcVersion = mcpModule.getSettings().minecraftVersion
                if (mcVersion == null || SemanticVersion.parse(mcVersion) > McpModuleType.MC_1_12_2) {
                    return
                }

                if (aClass.isMixin) {
                    return
                }

                val constructors = aClass.constructors
                for (constructor in constructors) {
                    if (constructor.parameterList.parameters.size != 1) {
                        continue
                    }

                    val parameter = constructor.parameterList.parameters[0]
                    val typeElement = parameter.typeElement ?: continue

                    val type = typeElement.type as? PsiClassType ?: continue

                    val resolve = type.resolve() ?: continue

                    if (resolve.qualifiedName == null) {
                        continue
                    }

                    if (resolve.qualifiedName == McpConstants.WORLD) {
                        return
                    }
                }

                registerClassError(aClass)
            }
        }
    }
}
