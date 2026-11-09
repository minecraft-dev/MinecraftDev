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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMember
import org.objectweb.asm.tree.ClassNode

/** Supplies shadow targets contributed by other transformations of the same target class. */
interface ShadowTargetResolver {
    fun resolveTarget(
        shadow: PsiAnnotation,
        targetClass: ClassNode,
        member: PsiMember,
        effectiveName: String,
    ): List<MixinTargetMember>

    companion object {
        val EP_NAME = ExtensionPointName.create<ShadowTargetResolver>("com.demonwav.minecraft-dev.shadowTargetResolver")
    }
}
