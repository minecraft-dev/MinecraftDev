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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.inspection.injector.CtorHeadNoUnsafeInspection
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.findSuperConstructorCall
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.isFabricMixin
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.enumValueOfOrNull
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findInspection
import com.demonwav.mcdev.util.mapToArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiStatement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt
import com.intellij.util.JavaPsiConstructorUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

class CtorHeadInjectionPoint : InjectionPoint<PsiElement>() {
    companion object {
        private val ARGS_KEYS = arrayOf("enforce")
    }

    override fun onCompleted(editor: Editor, reference: PsiLiteral) {
        val project = reference.project

        // avoid adding unsafe = true when it's unnecessary on Fabric
        val noUnsafeInspection =
            project.findInspection<CtorHeadNoUnsafeInspection>(CtorHeadNoUnsafeInspection.SHORT_NAME)
        if (reference.isFabricMixin && noUnsafeInspection?.ignoreForFabric == true) {
            return
        }

        val at = reference.parentOfType<PsiAnnotation>() ?: return
        at.setDeclaredAttributeValue(
            "unsafe",
            JavaPsiFacade.getElementFactory(project).createLiteralExpression(true)
        )
        CodeStyleManager.getInstance(project).reformat(at)
    }

    override fun getArgsKeys(at: PsiAnnotation) = ARGS_KEYS
    override fun getArgsValues(at: PsiAnnotation, key: String): Array<Any> = if (key == "enforce") {
        EnforceMode.values().mapToArray { it.name }
    } else {
        ArrayUtilRt.EMPTY_OBJECT_ARRAY
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor {
        val args = AtResolver.getArgs(at)
        val enforce = args["enforce"]?.let { enumValueOfOrNull<EnforceMode>(it) } ?: EnforceMode.DEFAULT
        return MyNavigationVisitor(enforce)
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement> {
        val args = AtResolver.getArgs(at)
        val enforce = args["enforce"]?.let { enumValueOfOrNull<EnforceMode>(it) } ?: EnforceMode.DEFAULT
        return MyCollectVisitor(at.project, targetClass, mode, enforce)
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>
    ): LookupElementBuilder? {
        return null
    }

    enum class EnforceMode {
        DEFAULT, POST_DELEGATE, POST_INIT
    }

    private class MyCollectVisitor(
        project: Project,
        clazz: ClassNode,
        mode: Mode,
        private val enforce: EnforceMode,
    ) : HeadInjectionPoint.MyCollectVisitor(project, clazz, mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return

            if (!methodNode.isConstructor) {
                super.accept(methodNode)
                return
            }

            val superCtorCall = methodNode.findSuperConstructorCall() ?: run {
                super.accept(methodNode)
                return
            }

            if (enforce == EnforceMode.POST_DELEGATE) {
                val insn = superCtorCall.next ?: return
                addResult(insn, methodNode.findOrConstructSourceMethod(clazz, project))
                return
            }

            // Although Mumfrey's original intention was to target the last *unique* field store,
            // i.e. ignore duplicate field stores that occur later, due to a bug in the implementation
            // it simply finds the last PUTFIELD whose owner is the target class. Mumfrey now says he
            // doesn't want to change the implementation in case of breaking mixins that rely on this
            // behavior, so it is now effectively intended, so it's what we'll use here.
            val lastFieldStore = generateSequence(insns.last) { it.previous }
                .takeWhile { it !== superCtorCall }
                .firstOrNull { insn ->
                    insn.opcode == Opcodes.PUTFIELD &&
                        (insn as FieldInsnNode).owner == clazz.name
                } ?: superCtorCall

            val lastFieldStoreNext = lastFieldStore.next ?: return
            addResult(lastFieldStoreNext, methodNode.findOrConstructSourceMethod(clazz, project))
        }
    }

    private class MyNavigationVisitor(private val enforce: EnforceMode) : NavigationVisitor() {
        private var isConstructor = true
        private var firstStatement = true
        private lateinit var elementToReturn: PsiElement

        override fun visitStart(executableElement: PsiElement) {
            isConstructor = executableElement is PsiMethod && executableElement.isConstructor
            elementToReturn = executableElement
        }

        override fun visitExpression(expression: PsiExpression) {
            if (firstStatement) {
                elementToReturn = expression
                firstStatement = false
            }
            super.visitExpression(expression)
        }

        override fun visitStatement(statement: PsiStatement) {
            if (firstStatement) {
                elementToReturn = statement
                firstStatement = false
            }
            super.visitStatement(statement)
        }

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            super.visitMethodCallExpression(expression)
            if (isConstructor) {
                if (JavaPsiConstructorUtil.isChainedConstructorCall(expression) ||
                    JavaPsiConstructorUtil.isSuperConstructorCall(expression)
                ) {
                    elementToReturn = expression
                }
            }
        }

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            super.visitReferenceExpression(expression)
            if (isConstructor &&
                enforce != EnforceMode.POST_DELEGATE &&
                expression !is PsiMethodReferenceExpression &&
                PsiUtil.isAccessedForWriting(expression)
            ) {
                val resolvedField = expression.resolve()
                if (resolvedField is PsiField && resolvedField.containingClass == expression.findContainingClass()) {
                    elementToReturn = expression
                }
            }
        }

        override fun visitEnd(executableElement: PsiElement) {
            addResult(elementToReturn)
        }
    }
}
