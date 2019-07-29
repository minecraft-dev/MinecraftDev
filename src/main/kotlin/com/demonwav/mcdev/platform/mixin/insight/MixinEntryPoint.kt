/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
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
    var MIXIN_ENTRY_POINT = true

    override fun getId() = "mixin"
    override fun getDisplayName() = "Mixin injectors"
    override fun getTitle() = "Suggest private visibility level for Mixin injectors"

    override fun getIgnoreAnnotations() = MixinConstants.Annotations.ENTRY_POINTS

    override fun isEntryPoint(element: PsiElement): Boolean {
        val modifierList = (element as? PsiMethod)?.modifierList ?: return false
        return MixinConstants.Annotations.ENTRY_POINTS.any {
            modifierList.findAnnotation(it) != null
        }
    }

    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement) = isEntryPoint(psiElement)

    override fun getMinVisibilityLevel(member: PsiMember): Int {
        if (member !is PsiMethod) return -1
        val modifierList = member.modifierList
        return if (MixinConstants.Annotations.METHOD_INJECTORS.any { modifierList.findAnnotation(it) != null }) {
            PsiUtil.ACCESS_LEVEL_PRIVATE
        } else {
            -1
        }
    }

    override fun isSelected() = MIXIN_ENTRY_POINT
    override fun setSelected(selected: Boolean) {
        MIXIN_ENTRY_POINT = selected
    }

    override fun readExternal(element: Element) = XmlSerializer.serializeInto(this, element)
    override fun writeExternal(element: Element) = XmlSerializer.serializeInto(this, element)
}
