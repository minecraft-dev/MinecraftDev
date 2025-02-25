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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.OPERATION
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.resolveClassArray
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDisjunctionType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.TypeConversionUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * Returns whether the given [PsiClass] is a Mixin class with a `@Mixin` annotation.
 *
 * @receiver The class to check
 * @return True if the given class is a Mixin
 */
val PsiClass.isMixin
    get() = mixinAnnotation != null

/**
 * Get the Mixin [PsiAnnotation] for the provided Mixin [PsiClass].
 * Returns null if the provided class is null or the class is not a Mixin class.
 *
 * @receiver The [PsiClass] to check.
 * @return The Mixin [PsiAnnotation] for the provided Mixin [PsiClass].
 */
val PsiClass.mixinAnnotation
    get() = modifierList?.findAnnotation(MIXIN)

/**
 * Get a list of every PsiClass target defined in the Mixin annotation of the given class.
 * Returns an empty list if this is not a Mixin class or if there are no resolvable targets defined in the Mixin annotation.
 *
 * @receiver The [PsiClass] to check.
 * @return A list of resolved classes defined in the Mixin targets.
 */
val PsiClass.mixinTargets: List<ClassNode>
    get() {
        return cached {
            val mixinAnnotation = mixinAnnotation ?: return@cached emptyList()

            // Read class targets (value)
            val classTargets =
                mixinAnnotation.findDeclaredAttributeValue(null)?.resolveClassArray()
                    ?.mapNotNullTo(mutableListOf()) { it.bytecode } ?: mutableListOf()

            // Read and add string targets (targets)
            mixinAnnotation.findDeclaredAttributeValue("targets")?.computeStringArray()
                ?.mapNotNullTo(classTargets) { name ->
                    findClassNodeByQualifiedName(
                        project,
                        findModule(),
                        name.replace('/', '.'),
                    )
                }
            classTargets
        }
    }

val PsiClass.bytecode: ClassNode?
    get() = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        findClassNodeByPsiClass(this)
    }

/**
 * Checks if the given [PsiClass] is an accessor Mixin. Return true if and only if:
 *
 * 1. The class given is a Mixin.
 * 2. The class given is an interface.
 * 3. All member methods are decorated with either `@Accessor` or `@Invoker`.
 * 4. All Mixin targets are classes.
 *
 * @receiver The class to check
 * @return True if the above checks are satisfied.
 */
val PsiClass.isAccessorMixin: Boolean
    get() {
        if (!isInterface) {
            return false
        }
        if (
            methods.any {
                it.modifierList.findAnnotation(ACCESSOR) == null &&
                    it.modifierList.findAnnotation(INVOKER) == null
            }
        ) {
            return false
        }

        val targets = mixinTargets
        return targets.isNotEmpty() && !targets.any { it.hasAccess(Opcodes.ACC_INTERFACE) }
    }

val PsiParameter.isMixinExtrasSugar: Boolean
    get() {
        return annotations.any { it.qualifiedName?.contains(".mixinextras.sugar.") == true }
    }

fun callbackInfoType(project: Project): PsiType =
    PsiType.getTypeByName(CALLBACK_INFO, project, GlobalSearchScope.allScope(project))

fun callbackInfoReturnableType(project: Project, context: PsiElement, returnType: PsiType): PsiType? {
    val boxedType = if (returnType is PsiPrimitiveType) {
        returnType.getBoxedType(context) ?: return null
    } else {
        returnType
    }

    return JavaPsiFacade.getElementFactory(project)
        .createTypeFromText("$CALLBACK_INFO_RETURNABLE<${boxedType.canonicalText}>", context)
}

fun mixinExtrasOperationType(context: PsiElement, type: PsiType): PsiType? {
    val project = context.project
    val boxedType = if (type is PsiPrimitiveType) {
        type.getBoxedType(context) ?: return null
    } else {
        type
    }

    return JavaPsiFacade.getElementFactory(project)
        .createTypeFromText("$OPERATION<${boxedType.canonicalText}>", context)
}

fun isAssignable(left: PsiType, right: PsiType, allowPrimitiveConversion: Boolean = true): Boolean {
    return when {
        left is PsiIntersectionType -> left.conjuncts.all { isAssignable(it, right) }
        right is PsiIntersectionType -> right.conjuncts.any { isAssignable(left, it) }
        left is PsiDisjunctionType -> left.disjunctions.any { isAssignable(it, right) }
        right is PsiDisjunctionType -> isAssignable(left, right.leastUpperBound)
        left is PsiArrayType -> right is PsiArrayType && isAssignable(left.componentType, right.componentType)
        else -> {
            if (left !is PsiClassType || right !is PsiClassType) {
                if (right == PsiTypes.nullType() && left !is PsiPrimitiveType) {
                    return true
                }
                if (!allowPrimitiveConversion && (left is PsiPrimitiveType || right is PsiPrimitiveType)) {
                    return left == right
                }
                return TypeConversionUtil.isAssignable(left, right)
            }
            val leftClass = left.resolve() ?: return false
            val rightClass = right.resolve() ?: return false

            val isLeftMixin = leftClass.isMixin
            val isRightMixin = rightClass.isMixin
            if (isLeftMixin || isRightMixin) {
                fun getClassesToTest(clazz: PsiClass, isMixin: Boolean) = if (isMixin) {
                    clazz.mixinTargets.mapNotNull { it.findStubClass(clazz.project) }
                } else {
                    listOf(clazz)
                }

                val leftClassesToTest = getClassesToTest(leftClass, isLeftMixin)
                val rightClassesToTest = getClassesToTest(rightClass, isRightMixin)

                val isMixinAssignable = leftClassesToTest.any { leftToTest ->
                    rightClassesToTest.any { rightToTest ->
                        isClassAssignable(leftToTest, rightToTest)
                    }
                }

                if (isMixinAssignable) {
                    return true
                }
            }

            val mixins = FindMixinsAction.findMixins(rightClass, rightClass.project) ?: return false
            if (mixins.any { isClassAssignable(leftClass, it) }) {
                return true
            }

            return isClassAssignable(leftClass, rightClass)
        }
    }
}

private fun isClassAssignable(leftClass: PsiClass, rightClass: PsiClass): Boolean {
    var result = false
    InheritanceUtil.processSupers(rightClass, true) {
        if (it.qualifiedName == leftClass.qualifiedName) {
            result = true
            false
        } else {
            true
        }
    }
    return result
}

fun isMixinEntryPoint(element: PsiElement?): Boolean {
    if (element !is PsiMethod) {
        return false
    }
    val project = element.project
    for (annotation in element.annotations) {
        val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, project)
        if (handler != null && handler.isEntryPoint) {
            return true
        }
    }
    return false
}

val PsiElement.isFabricMixin: Boolean get() =
    JavaPsiFacade.getInstance(project).findClass(MixinConstants.Classes.FABRIC_UTIL, resolveScope) != null
