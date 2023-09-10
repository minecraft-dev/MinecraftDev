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

package com.demonwav.mcdev.inspection

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.psi.PsiElement
import org.jdom.Element

class PlatformAnnotationEntryPoint : EntryPoint() {
    override fun getDisplayName() = MCDevBundle("inspection.entry_point.name")
    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement) = false
    override fun isEntryPoint(psiElement: PsiElement) = false
    override fun isSelected() = false
    override fun setSelected(selected: Boolean) {}
    override fun getIgnoreAnnotations() =
        arrayOf("org.spongepowered.api.event.Listener", "org.bukkit.event.EventHandler")

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
    }
}
