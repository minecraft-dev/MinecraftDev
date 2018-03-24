/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.inspection

import com.demonwav.mcdev.platform.canary.util.CanaryConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class CanaryListenerImplementedInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Canary @HookHandler in class not implementing PluginListener"
    override fun buildErrorString(vararg infos: Any) = "This class contains @HookHandler methods but does not implement PluginListener."
    override fun getStaticDescription() = "All Canary @HookHandler methods must reside in a class that implements PluginListener."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val psiClass = infos[0] as PsiClass
                psiClass.addImplements(CanaryConstants.LISTENER_CLASS)
            }

            @Nls
            override fun getName() = "Implement PluginListener"
            @Nls
            override fun getFamilyName() = name
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                val methods = aClass.methods
                val isEventHandler = methods.any { it.modifierList.findAnnotation(CanaryConstants.HOOK_HANDLER_ANNOTATION) != null }

                if (!isEventHandler) {
                    return
                }

                val inError = !aClass.extendsOrImplements(CanaryConstants.LISTENER_CLASS)

                if (inError) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
