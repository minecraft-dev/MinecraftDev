/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.inspection

import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class BungeeCordListenerImplementedInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "BungeeCord @EventHandler in class not implementing Listener"

    override fun buildErrorString(vararg infos: Any) =
        "This class contains @EventHandler methods but does not implement Listener"

    override fun getStaticDescription() =
        "All BungeeCord @EventHandler methods must reside in a class that implements Listener."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val psiClass = infos[0] as PsiClass
                psiClass.addImplements(BungeeCordConstants.LISTENER_CLASS)
            }

            @Nls
            override fun getName() = "Implement Listener"

            @Nls
            override fun getFamilyName() = name
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                val methods = aClass.methods
                val isEventHandler =
                    methods.any { it.modifierList.findAnnotation(BungeeCordConstants.HANDLER_ANNOTATION) != null }

                if (!isEventHandler) {
                    return
                }

                val inError = !aClass.extendsOrImplements(BungeeCordConstants.LISTENER_CLASS)

                if (inError) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
