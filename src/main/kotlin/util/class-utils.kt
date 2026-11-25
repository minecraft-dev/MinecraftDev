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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodFix
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil

val PsiClass.packageName
    get() = (containingFile as? PsiJavaFile)?.packageName

// Type

val PsiClassType.fullQualifiedName
    get() = resolve()?.fullQualifiedName // this can be null if the type import is missing

// Class

val PsiClass.outerQualifiedName
    get() = if (containingClass == null) qualifiedName else null

val PsiClass.fullQualifiedName
    get(): String? {
        (DesugarUtil.getOriginalElement(this) as? PsiClass)?.let { return it.fullQualifiedName }
        return try {
            outerQualifiedName ?: buildQualifiedName(StringBuilder()).toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

@Throws(ClassNameResolutionFailedException::class)
private fun PsiClass.buildQualifiedName(builder: StringBuilder): StringBuilder {
    if (this is PsiTypeParameter) {
        throw ClassNameResolutionFailedException()
    }
    buildInnerName(builder, PsiClass::outerQualifiedName)
    return builder
}

private val PsiClass.outerShortName
    get() = if (containingClass == null) name else null

val PsiClass.shortName: String?
    get() {
        if (this is PsiTypeParameter) {
            return null
        }
        outerShortName?.let { return it }
        return try {
            val builder = StringBuilder()
            buildInnerName(builder, PsiClass::outerShortName, '.')
            return builder.toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

@Throws(ClassNameResolutionFailedException::class)
inline fun PsiClass.buildInnerName(builder: StringBuilder, getName: (PsiClass) -> String?, separator: Char = '$') {
    var currentClass: PsiClass = this
    var parentClass: PsiClass?
    var name: String?
    val list = ArrayList<String>()

    do {
        parentClass = currentClass.containingClass
        if (parentClass != null) {
            // Add named inner class
            list.add(currentClass.name ?: throw ClassNameResolutionFailedException())
        } else {
            parentClass = currentClass.parent.findContainingClass() ?: throw ClassNameResolutionFailedException()

            if (currentClass is PsiAnonymousClass) {
                // Add index of anonymous class to list
                list.add(parentClass.getAnonymousIndex(currentClass).toString())
            } else {
                // Add index and name of local class to list
                val currentName = currentClass.name ?: throw ClassNameResolutionFailedException()
                list.add(parentClass.getLocalIndex(currentClass).toString() + currentName)
            }
        }

        currentClass = parentClass
        name = getName(currentClass)
    } while (name == null)

    // Append name of outer class
    builder.append(name)

    // Append names for all inner classes
    for (i in list.lastIndex downTo 0) {
        builder.append(separator).append(list[i])
    }
}

fun findQualifiedClass(fullQualifiedName: String, context: PsiElement): PsiClass? {
    return findQualifiedClass(context.project, fullQualifiedName, context.resolveScope)
}

fun findQualifiedClass(
    project: Project,
    fullQualifiedName: String,
    scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
): PsiClass? {
    return findQualifiedClass(fullQualifiedName) { name ->
        JavaPsiFacade.getInstance(project).findClass(name, scope)
    }
}

fun findQualifiedClass(
    fullQualifiedName: String,
    outerResolver: (String) -> PsiClass?,
): PsiClass? {
    var innerPos = fullQualifiedName.indexOf('$')
    if (innerPos == -1) {
        return outerResolver(fullQualifiedName)
    }

    var currentClass = outerResolver(fullQualifiedName.take(innerPos)) ?: return null
    var outerPos: Int

    while (true) {
        outerPos = innerPos + 1
        innerPos = fullQualifiedName.indexOf('$', outerPos)

        if (innerPos == -1) {
            return currentClass.findInnerClass(fullQualifiedName.substring(outerPos))
        } else {
            currentClass = currentClass.findInnerClass(fullQualifiedName.substring(outerPos, innerPos)) ?: return null
        }
    }
}

private fun PsiClass.findInnerClass(name: String): PsiClass? {
    val innerIndexAndName = innerIndexAndName(name)
    return if (innerIndexAndName == null) {
        // Named inner class
        findInnerClassByName(name, false)
    } else {
        val (innerIndex, innerName) = innerIndexAndName
        if (innerName != null) {
            val localClasses = this.localClasses.filter { it.name == innerName }
            if (innerIndex > 0 && innerIndex <= localClasses.size) {
                localClasses[innerIndex - 1]
            } else {
                null
            }
        } else {
            if (innerIndex > 0 && innerIndex <= anonymousClasses.size) {
                anonymousClasses[innerIndex - 1]
            } else {
                null
            }
        }
    }
}

@Throws(ClassNameResolutionFailedException::class)
@PublishedApi
internal fun PsiClass.getAnonymousIndex(anonymousElement: PsiElement): Int {
    // Attempt to find name for anonymous class
    for ((i, element) in anonymousClasses.withIndex()) {
        if (element equivalentTo anonymousElement) {
            return i + 1
        }
    }

    throw ClassNameResolutionFailedException("Failed to determine anonymous class for $anonymousElement")
}

val PsiClass.anonymousClasses: List<PsiAnonymousClass>
    get() {
        val list = mutableListOf<PsiAnonymousClass>()

        acceptChildren(object : JavaRecursiveElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                // skip inner classes
            }

            override fun visitAnonymousClass(aClass: PsiAnonymousClass) {
                aClass.argumentList?.accept(this)
                list += aClass
            }
        })

        return list
    }

@Throws(ClassNameResolutionFailedException::class)
@PublishedApi
internal fun PsiElement.getLocalIndex(localClass: PsiClass): Int {
    // Attempt to find index for local class
    var index = 0
    for (aLocalClass in localClasses) {
        if (aLocalClass.name == localClass.name) {
            index++
            if (aLocalClass equivalentTo localClass) {
                return index
            }
        }
    }

    throw ClassNameResolutionFailedException("Failed to determine local class index for $localClass")
}

val PsiElement.localClasses: List<PsiClass>
    get() {
        if (this is PsiCompiledElement) {
            return emptyList()
        }

        val list = mutableListOf<PsiClass>()
        accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(clazz: PsiClass) {
                if (clazz === this@localClasses) {
                    super.visitClass(clazz)
                } else {
                    if (PsiUtil.isLocalClass(clazz)) {
                        list += clazz
                    }
                }
            }
        })
        return list
    }

/**
 * The classes directly inside this class, including local and anonymous classes, paired with their binary names
 * relative to this class (the part after the `$`). For example, the first local class named `Foo` is `1Foo`, and the
 * second anonymous class is `2`.
 */
val PsiClass.namedAndUnnamedInnerClasses: List<Pair<String, PsiClass>>
    get() {
        val named = innerClasses.mapNotNull { innerClass -> innerClass.name?.let { it to innerClass } }
        val localIndices = HashMap<String, Int>()
        val local = localClasses.mapNotNull { localClass ->
            val name = localClass.name ?: return@mapNotNull null
            val index = localIndices.merge(name, 1, Int::plus)
            "$index$name" to localClass
        }
        val anonymous = anonymousClasses.mapIndexed { index, anonymousClass -> "${index + 1}" to anonymousClass }
        return named + local + anonymous
    }

private val INNER_INDEX_AND_NAME_REGEX = "(\\d+)(\\w*)".toRegex()
fun innerIndexAndName(className: String): Pair<Int, String?>? {
    val (indexStr, name) = INNER_INDEX_AND_NAME_REGEX.matchEntire(className)?.destructured ?: return null
    val index = indexStr.toIntOrNull() ?: return null
    return index to name.ifEmpty { null }
}

// Inheritance

fun PsiClass.extendsOrImplements(qualifiedClassName: String): Boolean {
    val aClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, resolveScope) ?: return false
    return equivalentTo(aClass) || this.isInheritor(aClass, true)
}

fun PsiClass.addImplements(qualifiedClassName: String) {
    if (interfaces.any { it.qualifiedName == qualifiedClassName }) {
        return
    }

    val project = project
    val listenerClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, resolveScope) ?: return

    val elementFactory = JavaPsiFacade.getElementFactory(project)
    val element = elementFactory.createClassReferenceElement(listenerClass)

    val referenceList = implementsList
    if (referenceList != null) {
        referenceList.add(element)
    } else {
        add(elementFactory.createReferenceList(arrayOf(element)))
    }
}

// Member

/**
 * Adds the given method to this class, or its copy. Returns the method actually added
 */
fun PsiClass.addMethod(template: PsiMethod): PsiMethod? {
    return AddMethodFix(template, this).createMethod(this)
}

fun PsiClass.findMatchingMethod(
    pattern: PsiMethod,
    checkBases: Boolean,
    name: String = pattern.name,
    constructor: Boolean = pattern.isConstructor,
): PsiMethod? {
    return findMethodsByName(name, checkBases).firstOrNull { it.isMatchingMethod(pattern, constructor) }
}

fun PsiClass.findMatchingMethods(
    pattern: PsiMethod,
    checkBases: Boolean,
    name: String = pattern.name,
    constructor: Boolean = pattern.isConstructor,
): List<PsiMethod> {
    return findMethodsByName(name, checkBases).filter { it.isMatchingMethod(pattern, constructor) }
}

inline fun PsiClass.findMatchingMethods(
    pattern: PsiMethod,
    checkBases: Boolean,
    name: String,
    func: (PsiMethod) -> Unit,
) {
    for (method in findMethodsByName(name, checkBases)) {
        if (method.isMatchingMethod(pattern)) {
            func(method)
        }
    }
}

fun PsiMethod.isMatchingMethod(pattern: PsiMethod, constructor: Boolean = pattern.isConstructor): Boolean {
    return this.isConstructor == constructor &&
        areReallyOnlyParametersErasureEqual(this.parameterList, pattern.parameterList) &&
        (this.isConstructor || constructor || this.returnType.isErasureEquivalentTo(pattern.returnType))
}

fun PsiClass.findMatchingField(pattern: PsiField, checkBases: Boolean, name: String = pattern.name): PsiField? {
    return try {
        findFieldByName(name, checkBases)?.takeIf { it.isMatchingField(pattern) }
    } catch (e: PsiInvalidElementAccessException) {
        null
    }
}

fun PsiField.isMatchingField(pattern: PsiField): Boolean {
    return type.isErasureEquivalentTo(pattern.type)
}

private fun areReallyOnlyParametersErasureEqual(
    parameterList1: PsiParameterList,
    parameterList2: PsiParameterList,
): Boolean {
    // Similar to MethodSignatureUtil.areParametersErasureEqual, but doesn't check method name
    if (parameterList1.parametersCount != parameterList2.parametersCount) {
        return false
    }

    val parameters1 = parameterList1.parameters
    val parameters2 = parameterList2.parameters
    for (i in parameters1.indices) {
        val type1 = parameters1[i].type
        val type2 = parameters2[i].type

        if (type1 is PsiPrimitiveType && (type2 !is PsiPrimitiveType || type1 != type2)) {
            return false
        }

        if (!type1.isErasureEquivalentTo(type2)) {
            return false
        }
    }

    return true
}

fun PsiClass.isJavaOptional(): Boolean = this.qualifiedName == CommonClassNames.JAVA_UTIL_OPTIONAL

fun PsiClassType.isJavaOptional(): Boolean = this.fullQualifiedName == CommonClassNames.JAVA_UTIL_OPTIONAL

class ClassNameResolutionFailedException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
}
