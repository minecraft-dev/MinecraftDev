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

package com.demonwav.mcdev.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.lang.ref.WeakReference

/**
 * A pointer to a [PsiElement] within the subtree of another [PsiElement], that can survive copies of the parent
 * element and find the same child in the copy.
 */
class PsiChildPointer<T: PsiElement> internal constructor(
    private var element: WeakReference<T>,
    private val path: List<PathElement>
) {
    fun dereference(parent: PsiElement): T? {
        val element = element.get()
        if (element != null && PsiTreeUtil.isAncestor(parent, element, false)) {
            return element
        }

        var result = parent
        for ((index, type) in path) {
            result = generateSequence(result.firstChild) { it.nextSibling }
                .filter { it.javaClass === type }
                .drop(index)
                .firstOrNull()
                ?: return null
        }

        @Suppress("UNCHECKED_CAST") // we checked the Class, it's part of the path
        return (result as T).also { this.element = WeakReference(it) }
    }

    internal data class PathElement(val index: Int, val type: Class<out PsiElement>)
}

fun <T: PsiElement> PsiElement.createChildPointer(child: T): PsiChildPointer<T> {
    if (child === this) {
        return PsiChildPointer(WeakReference(this), emptyList())
    }

    val path = mutableListOf<PsiChildPointer.PathElement>()
    var element: PsiElement = child
    var parent = child.parent
    while (parent != null) {
        val type = element.javaClass
        val indexInParent = generateSequence(parent.firstChild) { it.nextSibling }
            .filter { it.javaClass === type }
            .indexOf(element)

        path += PsiChildPointer.PathElement(indexInParent, type)

        if (parent === this) {
            path.reverse()
            return PsiChildPointer(WeakReference(child), path)
        }

        element = parent
        parent = element.parent
    }

    throw IllegalArgumentException("$child is not a child of $this")
}
