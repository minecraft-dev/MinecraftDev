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

package com.demonwav.mcdev.platform.bungeecord.inspection

import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.createSmartPointer
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class BungeeCordListenerImplementedInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "BungeeCord @EventHandler in class not implementing Listener"

    override fun buildErrorString(vararg infos: Any) =
        "This class contains @EventHandler methods but does not implement Listener"

    override fun getStaticDescription() =
        "All BungeeCord @EventHandler methods must reside in a class that implements Listener."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix {
        val classPointer = (infos[0] as PsiClass).createSmartPointer()
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                classPointer.element?.addImplements(BungeeCordConstants.LISTENER_CLASS)
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
                val methods = aClass.methods
                val isEventHandler =
                    methods.any { it.modifierList.findAnnotation(BungeeCordConstants.HANDLER_ANNOTATION) != null }

                if (!isEventHandler) {
                    return
                }

                val inError = !aClass.extendsOrImplements(BungeeCordConstants.LISTENER_CLASS)

                if (inError) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
