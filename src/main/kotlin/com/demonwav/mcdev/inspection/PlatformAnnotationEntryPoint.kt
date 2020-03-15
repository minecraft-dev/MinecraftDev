/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.psi.PsiElement
import org.jdom.Element

class PlatformAnnotationEntryPoint : EntryPoint() {
    override fun getDisplayName() = "Minecraft Entry Point"
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
