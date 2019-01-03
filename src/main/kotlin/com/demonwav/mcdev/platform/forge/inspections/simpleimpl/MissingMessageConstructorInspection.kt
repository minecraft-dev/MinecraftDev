/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class MissingMessageConstructorInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "IMessage or IMessageHandler implementation without empty constructor"

    override fun buildErrorString(vararg infos: Any?) = staticDescription

    override fun getStaticDescription() =
        "All implementations of IMessage and IMessageHandler must have an empty constructor"

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        val messageClass = infos[0] as PsiClass

        return if (messageClass.isWritable) {
            AddEmptyConstructorInspectionGadgetsFix(messageClass, "Add empty constructor")
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (!SimpleImplUtil.isMessageOrHandler(aClass)) {
                    return
                }

                if (aClass.constructors.isNotEmpty() && aClass.constructors.none { !it.hasParameters() }) {
                    registerClassError(aClass, aClass)
                }
            }
        }
    }
}
