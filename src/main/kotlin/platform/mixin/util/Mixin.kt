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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.LOCAL_REF_PACKAGE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.OPERATION
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.resolveClassArray
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDisjunctionType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTypesUtil
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
 * 4. None of the Mixin targets are interfaces.
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

val PsiType.isLocalRef: Boolean
    get() {
        return PsiTypesUtil.getPsiClass(this)?.qualifiedName?.startsWith(LOCAL_REF_PACKAGE) == true
    }

fun PsiType.unwrapLocalRef(): PsiType {
    if (this !is PsiClassType) {
        return this
    }
    val qName = resolve()?.qualifiedName ?: return this
    if (!qName.startsWith(LOCAL_REF_PACKAGE)) {
        return this
    }
    return when (qName.substringAfterLast('.')) {
        "LocalBooleanRef" -> PsiTypes.booleanType()
        "LocalCharRef" -> PsiTypes.charType()
        "LocalDoubleRef" -> PsiTypes.doubleType()
        "LocalFloatRef" -> PsiTypes.floatType()
        "LocalIntRef" -> PsiTypes.intType()
        "LocalLongRef" -> PsiTypes.longType()
        "LocalShortRef" -> PsiTypes.shortType()
        "LocalRef" -> parameters.getOrNull(0) ?: this
        else -> this
    }
}

fun PsiType.wrapLocalRef(project: Project): PsiType {
    if (isLocalRef) {
        return this
    }

    val elementFactory = JavaPsiFacade.getElementFactory(project)
    return when (this) {
        PsiTypes.booleanType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalBooleanRef")
        PsiTypes.charType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalCharRef")
        PsiTypes.doubleType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalDoubleRef")
        PsiTypes.floatType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalFloatRef")
        PsiTypes.intType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalIntRef")
        PsiTypes.longType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalLongRef")
        PsiTypes.shortType() -> elementFactory.createTypeByFQClassName(LOCAL_REF_PACKAGE + "LocalShortRef")
        else -> {
            val typeElement = elementFactory.createTypeElementFromText(LOCAL_REF_PACKAGE + "LocalRef<?>", null)
            typeElement.innermostComponentReferenceElement!!
                .parameterList!!
                .typeParameterElements[0]!!
                .replace(elementFactory.createTypeElement(this))
            typeElement.type
        }
    }
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

            val mixins = FindMixinsAction.Util.findMixins(rightClass, rightClass.project) ?: return false
            if (mixins.any { isClassAssignable(leftClass, it) }) {
                return true
            }

            isClassAssignable(leftClass, rightClass)
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

val PsiElement.isFabricMixin: Boolean get() =
    JavaPsiFacade.getInstance(project).findClass(MixinConstants.Classes.FABRIC_UTIL, resolveScope) != null

val Module.fabricMixinCompatibility: Int?
    get() {
        val facade = JavaPsiFacade.getInstance(project)
        val fabricUtil = facade.findClass(MixinConstants.Classes.FABRIC_UTIL, moduleWithLibrariesScope)
            ?: return null
        val compatibilityLatestField = fabricUtil.findFieldByName("COMPATIBILITY_LATEST", false) ?: return null
        return compatibilityLatestField.initializer?.constantValue as? Int
    }

val Module.mixinVersion: SemanticVersion?
    get() {
        val facade = JavaPsiFacade.getInstance(project)
        val bootstrap = facade.findClass(MixinConstants.Classes.MIXIN_BOOTSTRAP, moduleWithLibrariesScope)
            ?: return null
        val versionField = bootstrap.findFieldByName("VERSION", false) ?: return null
        val version = (versionField.initializer as? PsiLiteralExpression)?.value as? String ?: return null
        return SemanticVersion.tryParse(version)
    }

fun PsiElement.hasNamedLocalVariables(className: String): Boolean {
    for (checker in MissingLVTChecker.EP_NAME.extensionList) {
        if (checker.hasMissingLVT(this, className)) {
            return false
        }
    }

    return true
}

fun PsiModifierListOwner.shouldDoMixinAccessChecks(): Boolean {
    if (this is PsiMethod && isConstructor) {
        return false
    }

    return true
}
