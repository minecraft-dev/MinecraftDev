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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import org.objectweb.asm.Opcodes

class InjectIntoConstructorInspection : MixinInspection() {
    @JvmField
    var allowOnFabric = true

    override fun createOptionsPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val checkbox = JCheckBox("Allow @Inject into constructors in Fabric", allowOnFabric)
        checkbox.addActionListener {
            allowOnFabric = checkbox.isSelected
        }
        panel.add(checkbox)
        return panel
    }

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        val isFabric = holder.file.findModule()?.let { MinecraftFacet.getInstance(it) }?.isOfType(FabricModuleType)
            ?: false
        if (isFabric && allowOnFabric) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                val injectAnnotation = method.findAnnotation(INJECT) ?: return
                val problemElement = injectAnnotation.nameReferenceElement ?: return
                val handler = MixinAnnotationHandler.forMixinAnnotation(INJECT) as? InjectorAnnotationHandler ?: return
                val targets = handler.resolveTarget(injectAnnotation)
                for (target in targets) {
                    if (target !is MethodTargetMember || !target.classAndMethod.method.isConstructor) {
                        continue
                    }
                    val (targetClass, targetMethod) = target.classAndMethod
                    val instructions = handler.resolveInstructions(injectAnnotation, targetClass, targetMethod)
                    if (instructions.any { it.insn.opcode != Opcodes.RETURN }) {
                        holder.registerProblem(
                            problemElement,
                            "Cannot inject into constructors at non-return instructions",
                        )
                        return
                    }
                }
            }
        }
    }

    override fun getStaticDescription() = "@Inject into Constructor"
}
