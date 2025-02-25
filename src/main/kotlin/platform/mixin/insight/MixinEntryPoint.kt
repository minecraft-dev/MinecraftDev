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

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.isMixinEntryPoint
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInspection.reference.RefElement
import com.intellij.codeInspection.visibility.EntryPointWithVisibilityLevel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

class MixinEntryPoint : EntryPointWithVisibilityLevel() {

    @JvmField
    var mixinEntryPoint = true

    override fun getId() = "mixin"
    override fun getDisplayName() = "Mixin injectors"
    override fun getTitle() = "Suggest private visibility level for Mixin injectors"

    // TODO: support more handlers than the builtin
    // need to find a way to access the project for that
    override fun getIgnoreAnnotations() =
        MixinAnnotationHandler.getBuiltinHandlers()
            .filter { (_, handler) -> handler.isEntryPoint }
            .map { (name, _) -> name }
            .toTypedArray()

    override fun isEntryPoint(element: PsiElement) = isMixinEntryPoint(element)

    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement) = isEntryPoint(psiElement)

    override fun getMinVisibilityLevel(member: PsiMember): Int {
        if (member !is PsiMethod) {
            return -1
        }
        val project = member.project
        for (annotation in member.annotations) {
            val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, project)
            if (handler is InjectorAnnotationHandler) {
                return PsiUtil.ACCESS_LEVEL_PRIVATE
            }
        }
        return -1
    }

    override fun isSelected() = mixinEntryPoint
    override fun setSelected(selected: Boolean) {
        mixinEntryPoint = selected
    }

    override fun readExternal(element: Element) = XmlSerializer.serializeInto(this, element)
    override fun writeExternal(element: Element) = XmlSerializer.serializeInto(this, element)
}
