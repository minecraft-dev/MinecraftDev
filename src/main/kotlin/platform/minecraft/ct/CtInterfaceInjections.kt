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

package com.demonwav.mcdev.platform.minecraft.ct

import com.demonwav.mcdev.platform.minecraft.ct.gen.psi.CtItfEntry
import com.demonwav.mcdev.util.findQualifiedClass
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object CtInterfaceInjections {
    private val TRANSITIVE_INJECTED_INTERFACES_KEY = Key<CachedValue<Set<String>>>("mcdev.ctTransitiveInjectedInterfaces")

    /**
     * Returns the binary names (e.g. `com.example.Outer$Inner`) of interfaces that are injected with
     * `transitive-inject-interface` by class tweaker files in the project.
     */
    fun getTransitivelyInjectedInterfaces(project: Project): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TRANSITIVE_INJECTED_INTERFACES_KEY,
            {
                CachedValueProvider.Result(
                    computeTransitivelyInjectedInterfaces(project),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    private fun computeTransitivelyInjectedInterfaces(project: Project): Set<String> {
        val psiManager = PsiManager.getInstance(project)
        val result = mutableSetOf<String>()
        for (file in FileTypeIndex.getFiles(CtFileType, GlobalSearchScope.projectScope(project))) {
            val ctFile = psiManager.findFile(file) as? CtFile ?: continue
            for (entry in PsiTreeUtil.findChildrenOfType(ctFile, CtItfEntry::class.java)) {
                if (!entry.isTransitive) {
                    continue
                }
                val interfaceName = entry.interfaceName ?: continue
                result += interfaceName.replace('/', '.')
            }
        }
        return result
    }

    /**
     * Returns whether the given interface is available on the classes it is injected into from dependent mods, either
     * because it is directly injected with `transitive-inject-interface`, or because it is a superinterface of such an
     * interface.
     */
    fun isTransitivelyInjected(iface: PsiClass): Boolean {
        val injected = getTransitivelyInjectedInterfaces(iface.project)
        if (injected.isEmpty()) {
            return false
        }

        if (ClassUtil.getJVMClassName(iface) in injected) {
            return true
        }

        return injected.any { name ->
            findQualifiedClass(iface.project, name)?.isInheritor(iface, true) == true
        }
    }
}
