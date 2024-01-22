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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.findSuperConstructorCall
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
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
        val checkbox = JCheckBox("Always allow @Inject into constructors in Fabric", allowOnFabric)
        checkbox.addActionListener {
            allowOnFabric = checkbox.isSelected
        }
        panel.add(checkbox)
        return panel
    }

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        val isFabric = holder.file.findModule()?.let { MinecraftFacet.getInstance(it) }?.isOfType(FabricModuleType)
            ?: false
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                val injectAnnotation = method.findAnnotation(INJECT) ?: return
                val problemElement = injectAnnotation.nameReferenceElement ?: return
                val handler = MixinAnnotationHandler.forMixinAnnotation(INJECT) as? InjectorAnnotationHandler ?: return
                val targets = handler.resolveTarget(injectAnnotation)

                val ats = injectAnnotation.findDeclaredAttributeValue("at")
                    ?.findAnnotations()
                    ?: emptyList()

                for (target in targets) {
                    if (target !is MethodTargetMember || !target.classAndMethod.method.isConstructor) {
                        continue
                    }
                    val (targetClass, targetMethod) = target.classAndMethod

                    for (at in ats) {
                        val isUnsafe = at.findDeclaredAttributeValue("unsafe")?.constantValue as? Boolean
                            ?: (isFabric && allowOnFabric)

                        val instructions = AtResolver(at, targetClass, targetMethod).resolveInstructions()
                        if (!isUnsafe && instructions.any { it.insn.opcode != Opcodes.RETURN }) {
                            val atClass = at.nameReferenceElement?.resolve() as? PsiClass
                            val atHasUnsafe = !atClass?.findMethodsByName("unsafe", false).isNullOrEmpty()

                            val quickFixes = if (atHasUnsafe) {
                                arrayOf(AddUnsafeFix(at))
                            } else {
                                emptyArray()
                            }

                            holder.registerProblem(
                                problemElement,
                                "Cannot inject into constructors at non-return instructions",
                                *quickFixes,
                            )
                            return
                        }

                        val superCtorCall = targetMethod.findSuperConstructorCall()
                        if (superCtorCall != null &&
                            instructions.any {
                                val insnIndex = targetMethod.instructions.indexOf(it.insn)
                                val superCtorIndex = targetMethod.instructions.indexOf(superCtorCall)
                                insnIndex <= superCtorIndex
                            }
                        ) {
                            holder.registerProblem(
                                problemElement,
                                "Cannot inject before super() call",
                            )
                            return
                        }
                    }
                }
            }
        }
    }

    override fun getStaticDescription() = "@Inject into Constructor"

    private class AddUnsafeFix(at: PsiAnnotation) : LocalQuickFixOnPsiElement(at) {
        override fun getFamilyName() = "Add unsafe = true"
        override fun getText() = "Add unsafe = true"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val annotation = startElement as? PsiAnnotation ?: return
            val trueExpr = JavaPsiFacade.getElementFactory(project).createLiteralExpression(true)
            annotation.setDeclaredAttributeValue("unsafe", trueExpr)
        }
    }
}
