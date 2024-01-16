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

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class MissingMessageConstructorInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "IMessage or IMessageHandler implementation without empty constructor"

    override fun buildErrorString(vararg infos: Any?) = staticDescription

    override fun getStaticDescription() =
        "All implementations of IMessage and IMessageHandler must have an empty constructor"

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        val messageClass = infos[0] as PsiClass

        return if (messageClass.isWritable) {
            AddEmptyConstructorInspectionGadgetsFix
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (!SimpleImplUtil.isMessageOrHandler(aClass)) {
                    return
                }

                if (aClass.constructors.isNotEmpty() && aClass.constructors.none { !it.hasParameters() }) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
