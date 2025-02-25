/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.isFabricMixin
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findInspection
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class UnnecessaryUnsafeInspection : MixinInspection() {
    @JvmField
    var alwaysUnnecessaryOnFabric = true

    override fun getStaticDescription() = "Reports unnecessary unsafe = true"

    override fun createOptionsPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val checkbox = JCheckBox("Always unnecessary when Fabric Mixin is present", alwaysUnnecessaryOnFabric)
        checkbox.addActionListener {
            alwaysUnnecessaryOnFabric = checkbox.isSelected
        }
        panel.add(checkbox)
        return panel
    }

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        val isFabric = holder.file.isFabricMixin
        val alwaysUnnecessary = isFabric && alwaysUnnecessaryOnFabric
        val requiresUnsafeForCtorHeadOnFabric =
            holder.project.findInspection<CtorHeadNoUnsafeInspection>(CtorHeadNoUnsafeInspection.SHORT_NAME)
                ?.ignoreForFabric == false

        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                    return
                }
                if ((!alwaysUnnecessary || requiresUnsafeForCtorHeadOnFabric) &&
                    annotation.findDeclaredAttributeValue("value")?.constantValue == "CTOR_HEAD"
                ) {
                    // this case is handled by a specific inspection for CTOR_HEAD
                    return
                }

                val unsafeValue = annotation.findDeclaredAttributeValue("unsafe") ?: return
                if (unsafeValue.constantValue != true) {
                    return
                }

                if (alwaysUnnecessary || !mightTargetConstructor(annotation)) {
                    holder.registerProblem(
                        unsafeValue,
                        "Unnecessary unsafe = true",
                        AnnotationAttributeFix(annotation, "unsafe" to null)
                    )
                }
            }
        }
    }

    companion object {
        fun mightTargetConstructor(at: PsiAnnotation): Boolean {
            val injectorAnnotation = AtResolver.findInjectorAnnotation(at) ?: return true

            val targets = MixinAnnotationHandler.resolveTarget(injectorAnnotation)
                .filterIsInstance<MethodTargetMember>()
                .ifEmpty { return true }

            return targets.any { it.classAndMethod.method.isConstructor }
        }
    }
}
