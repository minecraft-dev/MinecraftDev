/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isSpongePluginClass
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix
import com.intellij.codeInsight.daemon.impl.quickfix.ModifierFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier

class SpongePluginClassInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getStaticDescription() = "Checks the plugin class is valid."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return if (SpongeModuleType.isInModule(holder.file)) {
            Visitor(holder)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (SpongeModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            emptyList()
        }
    }

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            if (!aClass.isSpongePluginClass()) {
                return
            }

            val ctorInjectAnnos = aClass.constructors.mapNotNull {
                val annotation = it.getAnnotation(SpongeConstants.INJECT_ANNOTATION) ?: return@mapNotNull null
                it to annotation
            }
            if (ctorInjectAnnos.size > 1) {
                val quickFixFactory = QuickFixFactory.getInstance()
                ctorInjectAnnos.forEach { (injectedMethod, injectAnno) ->
                    holder.registerProblem(
                        injectAnno,
                        "There can only be one injected constructor.",
                        quickFixFactory.createDeleteFix(injectAnno, "Remove this @Inject"),
                        quickFixFactory.createDeleteFix(injectedMethod, "Remove this injected constructor")
                    )
                }
            }
            val hasInjectedCtor = ctorInjectAnnos.isNotEmpty()

            val emptyCtor = aClass.constructors.find { !it.hasParameters() }
            if (emptyCtor == null && !hasInjectedCtor) {
                val classIdentifier = aClass.nameIdentifier
                if (classIdentifier != null) {
                    holder.registerProblem(
                        classIdentifier,
                        "Plugin class must have an empty constructor or an @Inject constructor.",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddDefaultConstructorFix(aClass)
                    )
                }
            }

            if (!hasInjectedCtor && emptyCtor != null && emptyCtor.hasModifier(JvmModifier.PRIVATE)) {
                val ctorIdentifier = emptyCtor.nameIdentifier
                if (ctorIdentifier != null) {
                    holder.registerProblem(
                        ctorIdentifier,
                        "Plugin class empty constructor must not be private.",
                        ProblemHighlightType.GENERIC_ERROR,
                        ModifierFix(emptyCtor, PsiModifier.PACKAGE_LOCAL, true, false),
                        ModifierFix(emptyCtor, PsiModifier.PROTECTED, true, false),
                        ModifierFix(emptyCtor, PsiModifier.PUBLIC, true, false)
                    )
                }
            }

            aClass.getAnnotation(SpongeConstants.PLUGIN_ANNOTATION)?.let { pluginAnno ->
                val pluginIdValue = pluginAnno.findAttributeValue("id") ?: return@let
                val pluginId = pluginIdValue.constantStringValue ?: return@let
                if (!SpongeConstants.ID_PATTERN.matcher(pluginId).matches()) {
                    holder.registerProblem(
                        pluginIdValue,
                        "Plugin IDs should be lowercase, and only contain characters from a-z, dashes or underscores," +
                            " start with a lowercase letter, and not exceed 64 characters."
                    )
                }
            }
        }
    }
}
