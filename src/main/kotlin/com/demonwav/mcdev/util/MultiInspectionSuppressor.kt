/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.LanguageInspectionSuppressors
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiElement

/**
 * By default, IntelliJ IDEA will only consult the first [InspectionSuppressor]
 * in the list for each language. If registered as first [InspectionSuppressor],
 * this class will include all registered [InspectionSuppressor]s.
 *
 * TODO: Submit pull request to IntelliJ IDEA to change
 * [InspectionProfileEntry.getSuppressors] to use allForLanguage?
 */
class MultiInspectionSuppressor : InspectionSuppressor {

    private val suppressors: List<InspectionSuppressor>
        get() = LanguageInspectionSuppressors.INSTANCE.allForLanguage(JavaLanguage.INSTANCE)

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        return suppressors.any { it != this && it.isSuppressedFor(element, toolId) }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        val quickFixes = ArrayList<SuppressQuickFix>()
        for (suppressor in suppressors) {
            if (suppressor != this) {
                quickFixes.addAll(suppressor.getSuppressActions(element, toolId))
            }
        }

        if (quickFixes.isEmpty()) {
            return SuppressQuickFix.EMPTY_ARRAY
        }

        return quickFixes.toTypedArray()
    }

}
