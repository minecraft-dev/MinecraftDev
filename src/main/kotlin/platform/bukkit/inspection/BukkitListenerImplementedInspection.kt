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

package com.demonwav.mcdev.platform.bukkit.inspection

import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class BukkitListenerImplementedInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() =
        "Bukkit @EventHandler in class not implementing Listener"

    override fun buildErrorString(vararg infos: Any) =
        "This class contains @EventHandler methods but does not implement Listener."

    override fun getStaticDescription() =
        "All Bukkit @EventHandler methods must reside in a class that implements Listener."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix {
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val psiClass = infos[0] as PsiClass
                psiClass.addImplements(BukkitConstants.LISTENER_CLASS)
            }

            @Nls
            override fun getName() = "Implement Listener"

            @Nls
            override fun getFamilyName() = name
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (
                    aClass.methods.none {
                        it.modifierList.findAnnotation(BukkitConstants.HANDLER_ANNOTATION) != null
                    }
                ) {
                    return
                }

                val inError = !aClass.extendsOrImplements(BukkitConstants.LISTENER_CLASS)

                if (inError) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
