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
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.impl.compiled.ClsElementImpl

/**
 * A class, field or method that can have a Javadoc comment in annotater. All names are internal (slash separated)
 * names, and members are identified by their name and JVM descriptor.
 */
sealed class AnnotaterJavadocTarget {
    abstract val owner: String

    /**
     * The top level class whose `.mapping` file stores this target.
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
        /**
         * The target documented by [element], which may be an element of a class file's decompiled text.
         */
        fun fromPsi(element: PsiElement): AnnotaterJavadocTarget? {
            // Decompiled text is generated from the compiled class, which is more reliable to get the target of
            val compiled = element.getUserData(ClsElementImpl.COMPILED_ELEMENT) ?: element
            if (compiled !is PsiMember || !isDocumentable(compiled)) {
                return null
            }
            return when (compiled) {
                is PsiClass -> ClassTarget(compiled.internalName ?: return null)
                is PsiField -> {
                    val owner = compiled.containingClass?.internalName ?: return null
                    FieldTarget(owner, compiled.name, compiled.descriptor ?: return null)
                }

                is PsiMethod -> {
                    val owner = compiled.containingClass?.internalName ?: return null
                    MethodTarget(owner, compiled.internalName, compiled.descriptor ?: return null)
                }

                else -> null
            }
        }

        /**
         * The classes, fields and methods declared in the source of [file] in text order, including those in local
         * and anonymous classes, but not members that don't appear in the source, such as the implicit methods of
         * records. For a class file, these are the declarations in its decompiled text.
         */
        fun declarations(file: PsiJavaFile): List<PsiMember> {
            val source = (if (file is PsiCompiledElement) file.mirror else file) as? PsiJavaFile ?: return emptyList()
            return SyntaxTraverser.psiTraverser(source)
                .filter(PsiMember::class.java)
                .filter(::isDocumentable)
                .toList()
        }

        /**
         * Whether [member] is a kind of declaration that can be documented: a class, field or method, but not a type
         * parameter or anonymous class (including enum constant bodies), which are also [PsiClass]es.
         */
        private fun isDocumentable(member: PsiMember): Boolean = when (member) {
            is PsiTypeParameter, is PsiAnonymousClass -> false
            is PsiClass, is PsiField, is PsiMethod -> true
            else -> false
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
