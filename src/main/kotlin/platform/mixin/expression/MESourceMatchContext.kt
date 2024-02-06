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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class MESourceMatchContext(val project: Project) {
    @PublishedApi
    internal var realElement: PsiElement? = null
    private val capturesInternal = mutableListOf<PsiElement>()
    val captures: List<PsiElement> get() = capturesInternal

    private val types = mutableMapOf<String, MutableList<String>>()
    private val targetedElements = mutableMapOf<String, MutableList<PsiElement>>()
    private val localInfos = mutableMapOf<String, MutableList<LocalInfo>>()

    fun addCapture(capturedElement: PsiElement) {
        val element = realElement ?: capturedElement
        capturesInternal += element
    }

    fun getTypes(key: String): List<String> = types[key] ?: emptyList()

    fun addType(key: String, desc: String) {
        types.computeIfAbsent(key) { mutableListOf() } += desc
    }

    fun getTargetedElements(key: String): List<PsiElement> = targetedElements[key] ?: emptyList()

    fun addTargetedElement(key: String, element: PsiElement) {
        targetedElements.computeIfAbsent(key) { mutableListOf() } += element
    }

    fun getLocalInfos(key: String): List<LocalInfo> = localInfos[key] ?: emptyList()

    fun addLocalInfo(key: String, localInfo: LocalInfo) {
        localInfos.computeIfAbsent(key) { mutableListOf() } += localInfo
    }

    fun reset() {
        capturesInternal.clear()
    }

    inline fun <T> fakeElementScope(
        isFake: Boolean,
        realElement: PsiElement,
        action: () -> T
    ): T {
        if (this.realElement != null || !isFake) {
            return action()
        }

        this.realElement = realElement
        try {
            return action()
        } finally {
            this.realElement = null
        }
    }
}
