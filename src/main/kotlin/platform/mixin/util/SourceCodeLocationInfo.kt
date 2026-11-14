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

import com.demonwav.mcdev.util.findDocument
import com.demonwav.mcdev.util.lineNumber
import com.demonwav.mcdev.util.remapLineNumber
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Info returned from the bytecode to help locate an element from the source code.
 *
 * For example, if searching for a lambda in the source code, this contains the index of the lambda in the method (i.e.
 * how many lambdas were before this one), the starting line number of the lambda (or `null` if there were no line
 * numbers), and the index of the lambda within this line number (i.e. how many lambdas there were before this one in
 * the same line).
 *
 * The line number stored is unmapped, and may need remapping via [remapLineNumber].
 * [createMatcher] does this internally.
 */
class SourceCodeLocationInfo(val index: Int, val lineNumber: Int?, val indexInLineNumber: Int) {
    interface Matcher<T: PsiElement> {
        fun accept(t: T): Boolean

        val result: T?
    }

    fun <T: PsiElement> createMatcher(psiFile: PsiFile): Matcher<T> {
        val lineNumber = this.lineNumber?.let(psiFile::remapLineNumber)
        val document = psiFile.findDocument()

        return object : Matcher<T> {
            private var count = 0
            private var currentLine: Int? = null
            private var countThisLine = 0
            private var myResult: T? = null
            private var indexedResult: T? = null

            override fun accept(t: T): Boolean {
                val line = document?.let(t::lineNumber)
                if (line != null) {
                    if (line != currentLine) {
                        countThisLine = 0
                        currentLine = line
                    }

                    countThisLine++
                    if (line == lineNumber && countThisLine == indexInLineNumber + 1) {
                        myResult = t
                        return true
                    }
                }

                if (count++ == index) {
                    indexedResult = t
                    if (lineNumber == null) {
                        myResult = t
                        return true
                    }
                }

                return false
            }

            override val result get() = myResult ?: indexedResult
        }
    }
}
