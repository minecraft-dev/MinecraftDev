/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findParent
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

class VisualizeInjectorsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val data = getDataFromActionEvent(e) ?: return

        if (!data.instance.isOfType(MixinModuleType.getInstance())) {
            return
        }

        val psiClass = findParent<PsiClass>(data.element, true)
        val allMixedClasses = MixinUtils.getAllMixedClasses(psiClass)

        val parent = findParent<PsiMethod>(data.element, true) ?: return


    }
}
