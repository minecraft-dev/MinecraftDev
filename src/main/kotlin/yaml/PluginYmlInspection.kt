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

package com.demonwav.mcdev.yaml

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.bukkit.PaperModuleType
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class PluginYmlInspection : LocalInspectionTool() {

    override fun getStaticDescription(): @Nls String? = "Reports issues in Bukkit-like plugin.yml files"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val module = holder.file.findModule() ?: return PsiElementVisitor.EMPTY_VISITOR
        val virtualFile = holder.file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
        val pluginClassFqn = when {
            MinecraftFacet.getInstance(module, SpigotModuleType, PaperModuleType)?.pluginYml == virtualFile ->
                BukkitConstants.PLUGIN
            MinecraftFacet.getInstance(module, BungeeCordModuleType)?.pluginYml == virtualFile ->
                BungeeCordConstants.PLUGIN
            else -> return PsiElementVisitor.EMPTY_VISITOR
        }

        return Visitor(holder, pluginClassFqn)
    }

    private class Visitor(val holder: ProblemsHolder, val pluginClassFqn: String) : YamlPsiElementVisitor() {

        override fun visitScalar(scalar: YAMLScalar) {
            super.visitScalar(scalar)

            if ((scalar.parent as? YAMLKeyValue)?.keyText != "main") {
                return
            }

            val resolved = scalar.references.firstNotNullOfOrNull { it.resolve() as? PsiClass }
            if (resolved == null) {
                holder.registerProblem(scalar, "Unresolved reference")
                return
            }

            val pluginClass = JavaPsiFacade.getInstance(holder.project).findClass(pluginClassFqn, scalar.resolveScope)
                ?: return
            if (!resolved.isInheritor(pluginClass, true)) {
                holder.registerProblem(scalar, "Class must implement $pluginClassFqn")
            }
        }
    }
}
