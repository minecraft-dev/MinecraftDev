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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.internalName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

/**
 * A class, field or method that can have a Javadoc comment in annotater. All names are internal (slash separated)
 * official names, and members are identified by their name and JVM descriptor.
 */
sealed class AnnotaterJavadocTarget {
    abstract val owner: String

    /**
     * The internal name of the top level class whose `.mapping` file stores this target.
     */
    val outerClass: String
        get() = outerClassName(owner)

    data class ClassTarget(val internalName: String) : AnnotaterJavadocTarget() {
        override val owner get() = internalName
    }

    data class FieldTarget(
        override val owner: String,
        val name: String,
        val descriptor: String,
    ) : AnnotaterJavadocTarget()

    data class MethodTarget(
        override val owner: String,
        val name: String,
        val descriptor: String,
    ) : AnnotaterJavadocTarget()

    companion object {
        fun fromPsi(element: PsiElement): AnnotaterJavadocTarget? {
            return when (element) {
                is PsiClass -> {
                    if (element.qualifiedName == null) {
                        // Anonymous and local classes can't be documented
                        return null
                    }
                    ClassTarget(element.internalName ?: return null)
                }

                is PsiField -> {
                    val owner = element.containingClass?.takeIf { it.qualifiedName != null } ?: return null
                    FieldTarget(owner.internalName ?: return null, element.name, element.descriptor ?: return null)
                }

                is PsiMethod -> {
                    val owner = element.containingClass?.takeIf { it.qualifiedName != null } ?: return null
                    MethodTarget(
                        owner.internalName ?: return null,
                        element.internalName,
                        element.descriptor ?: return null
                    )
                }

                else -> null
            }
        }

        /**
         * Returns the top level class of a nested class name, treating `$` directly after a package separator or at
         * the start of a name as part of the class name, matching mcsrc.
         */
        fun outerClassName(name: String): String {
            var start = 0
            while (true) {
                val separator = name.indexOf('$', start)
                if (separator < 0) {
                    return name
                }
                if (separator > 0 && name[separator - 1] != '/') {
                    return name.substring(0, separator)
                }
                start = separator + 1
            }
        }
    }
}
