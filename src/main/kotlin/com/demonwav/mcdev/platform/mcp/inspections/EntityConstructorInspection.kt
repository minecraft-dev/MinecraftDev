/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.inspections

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.platform.mixin.util.isMixin
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

                val instance = MinecraftFacet.getInstance(module) ?: return

                if (!instance.isOfType(McpModuleType)) {
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
