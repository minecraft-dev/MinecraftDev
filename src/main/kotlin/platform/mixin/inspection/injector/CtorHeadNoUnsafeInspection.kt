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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isFabricMixin
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class CtorHeadNoUnsafeInspection : MixinInspection() {
    @JvmField
    var ignoreForFabric = true

    override fun createOptionsPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val checkbox = JCheckBox(
            MCDevBundle("inspection.ctor_head_no_unsafe.option.ignore_for_fabric"),
            ignoreForFabric,
        )
        checkbox.addActionListener {
            ignoreForFabric = checkbox.isSelected
        }
        panel.add(checkbox)
        return panel
    }

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        if (ignoreForFabric) {
            val isFabric = holder.file.isFabricMixin
            if (isFabric) {
                return PsiElementVisitor.EMPTY_VISITOR
            }
        }

        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                    return
                }
                val valueElement = annotation.findDeclaredAttributeValue("value")
                if (valueElement?.constantStringValue != "CTOR_HEAD") {
                    return
                }
                if (annotation.findDeclaredAttributeValue("unsafe")?.constantValue == true) {
                    return
                }
                holder.registerProblem(
                    valueElement,
                    MCDevBundle("inspection.ctor_head_no_unsafe.message"),
                    AnnotationAttributeFix(annotation, "unsafe" to true),
                )
            }
        }
    }

    companion object {
        const val SHORT_NAME = "CtorHeadNoUnsafe"
    }
}
