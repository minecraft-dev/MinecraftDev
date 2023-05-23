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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.creator.isValidClassName
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope

data class EntryPoint(
    val category: String,
    val type: Type,
    val className: String,
    val interfaceName: String,
    val methodName: String? = null,
) {
    private val dumbReference = when (type) {
        Type.CLASS -> className
        Type.METHOD -> "$className::$methodName"
    }

    val valid by lazy { category.isNotBlank() && isValidClassName(className) && isValidClassName(interfaceName) }

    fun computeReference(project: Project): String {
        if (type != Type.METHOD || methodName != null) {
            return dumbReference
        }
        return "$className::${findFunctionalMethod(project)?.name}"
    }

    fun findFunctionalMethod(project: Project): PsiMethod? {
        val classFinder = JavaPsiFacade.getInstance(project)
        val clazz = classFinder.findClass(className, GlobalSearchScope.projectScope(project)) ?: return null
        val interfaceClass = classFinder.findClass(interfaceName, clazz.resolveScope) ?: return null
        if (!interfaceClass.isInterface) {
            return null
        }
        val candidates = interfaceClass.allMethods
            .filter {
                !it.hasModifierProperty(PsiModifier.STATIC) &&
                    !it.hasModifierProperty(PsiModifier.DEFAULT) &&
                    it.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT
            }
        return if (candidates.size == 1) {
            candidates[0]
        } else {
            null
        }
    }

    override fun toString() = "$category -> $dumbReference implements $interfaceName"

    enum class Type {
        CLASS, METHOD
    }
}
