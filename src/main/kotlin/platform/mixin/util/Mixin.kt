/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.ARGS
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.resolveClassArray
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
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
                        name.replace('/', '.')
                    )
                }
            classTargets
        }
    }

val PsiClass.bytecode: ClassNode?
    get() = cached {
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

fun callbackInfoType(project: Project): PsiType =
    PsiType.getTypeByName(CALLBACK_INFO, project, GlobalSearchScope.allScope(project))

fun callbackInfoReturnableType(project: Project, context: PsiElement, returnType: PsiType): PsiType? {
    val boxedType = if (returnType is PsiPrimitiveType) {
        returnType.getBoxedType(context) ?: return null
    } else {
        returnType
    }

    // TODO: Can we do this without looking up the PsiClass?
    val psiClass =
        JavaPsiFacade.getInstance(project).findClass(CALLBACK_INFO_RETURNABLE, GlobalSearchScope.allScope(project))
            ?: return null
    return JavaPsiFacade.getElementFactory(project).createType(psiClass, boxedType)
}

fun argsType(project: Project): PsiType =
    PsiType.getTypeByName(ARGS, project, GlobalSearchScope.allScope(project))
