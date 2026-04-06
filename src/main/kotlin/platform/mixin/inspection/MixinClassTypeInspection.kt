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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.demonwav.mcdev.platform.mixin.util.isFabricMixin
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.java.syntax.parser.JavaKeywords
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiKeyword
import com.intellij.psi.tree.TokenSet
import org.objectweb.asm.Opcodes

class MixinClassTypeInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when a Mixin uses the wrong class type"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitClass(mixinClass: PsiClass) {
            if (!mixinClass.isMixin) {
                return
            }
            if (mixinClass.isAccessorMixin) {
                return
            }

            val classKeywordElement = mixinClass.children.firstOrNull {
                (it as? PsiKeyword)?.tokenType in Const.CLASS_KEYWORD_SET
            }
            val problemElement = classKeywordElement ?: mixinClass.nameIdentifier ?: mixinClass

            val needsToBeClass = mixinClass.mixinTargets.any { !it.hasAccess(Opcodes.ACC_INTERFACE) }
            val needsToBeInterface = mixinClass.mixinTargets.any { it.hasAccess(Opcodes.ACC_INTERFACE) }

            val fixes = mutableListOf<LocalQuickFix>()
            if (classKeywordElement != null) {
                if (needsToBeClass && !needsToBeInterface) {
                    fixes += ChangeClassTypeFix(classKeywordElement, JavaKeywords.CLASS)
                } else if (needsToBeInterface && !needsToBeClass) {
                    fixes += ChangeClassTypeFix(classKeywordElement, JavaKeywords.INTERFACE)
                }
            }

            if (mixinClass.isEnum && !mixinClass.isFabricMixin) {
                holder.registerProblem(problemElement, "Mixins cannot be enums", *fixes.toTypedArray())
                return
            }

            if (mixinClass.isAnnotationType) {
                holder.registerProblem(problemElement, "Mixins cannot be annotation types", *fixes.toTypedArray())
                return
            }

            if (mixinClass.isRecord) {
                holder.registerProblem(problemElement, "Mixins cannot be records", *fixes.toTypedArray())
                return
            }

            if (mixinClass.isInterface) {
                if (needsToBeClass) {
                    holder.registerProblem(problemElement, "Interface Mixin targets a class", *fixes.toTypedArray())
                }
            } else {
                if (needsToBeInterface) {
                    holder.registerProblem(problemElement, "Class Mixin targets an interface", *fixes.toTypedArray())
                }
            }
        }
    }

    private object Const {
        val CLASS_KEYWORD_SET = TokenSet.create(
            JavaTokenType.CLASS_KEYWORD,
            JavaTokenType.INTERFACE_KEYWORD,
            JavaTokenType.ENUM_KEYWORD,
            JavaTokenType.RECORD_KEYWORD,
        )
    }

    private class ChangeClassTypeFix(
        classKeywordElement: PsiElement,
        private val newKeyword: String
    ) : LocalQuickFixOnPsiElement(classKeywordElement) {
        override fun getFamilyName() = "Change class type to $newKeyword"
        override fun getText() = "Change class type to $newKeyword"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val factory = JavaPsiFacade.getElementFactory(project)
            startElement.replace(factory.createKeyword(newKeyword))
        }
    }
}
