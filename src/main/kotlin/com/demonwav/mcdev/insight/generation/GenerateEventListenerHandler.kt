/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.ui.EventGenerationDialog
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.util.castNotNull
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase
import com.intellij.codeInsight.generation.GenerationInfo
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle

/**
 * The standard handler to generate a new event listener as a method.
 * Note that this is a psuedo generator as it relies on a wizard and the
 * [.cleanup] to complete
 */
class GenerateEventListenerHandler : GenerateMembersHandlerBase("Generate Event Listener") {
    private var editor: Editor? = null
    private var position: LogicalPosition? = null
    private var method: PsiMethod? = null
    private var model: CaretModel? = null

    private var data: GenerationData? = null
    private var chosenClass: PsiClass? = null
    private var chosenName: String? = null
    private var relevantModule: AbstractModule? = null
    private var okay: Boolean = false

    override fun getHelpId() = "Generate Event Listener Dialog"

    override fun chooseOriginalMembers(aClass: PsiClass, project: Project, editor: Editor?): Array<ClassMember>? {
        this.editor = editor

        val moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return null

        val facet = MinecraftFacet.getInstance(moduleForPsiElement) ?: return null

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(RefactoringBundle.message("choose.destination.class"),
                                                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(moduleForPsiElement, false),
                                                { aClass1 -> isSuperEventListenerAllowed(aClass1, facet) }, null
            )

        chooser.showDialog()
        chosenClass = chooser.selected

        chosenClass?.let { chosenClass ->
            val relevantModule = facet.modules.asSequence()
                .filter { m -> isSuperEventListenerAllowed(chosenClass, m) }
                .firstOrNull() ?: return null

            this.relevantModule = relevantModule

            val generationDialog = EventGenerationDialog(
                editor!!,
                relevantModule.moduleType.getEventGenerationPanel(chosenClass),
                chosenClass.nameIdentifier!!.text,
                relevantModule.moduleType.getDefaultListenerName(chosenClass)
            )

            okay = generationDialog.showAndGet()

            if (!okay) {
                return null
            }

            data = generationDialog.data
            chosenName = generationDialog.chosenName

            model = editor.caretModel
            position = model!!.logicalPosition

            method = PsiTreeUtil.getParentOfType(aClass.containingFile.findElementAt(model!!.offset), PsiMethod::class.java)
        }

        return DUMMY_RESULT.castNotNull()
    }

    override fun getAllOriginalMembers(aClass: PsiClass) = null

    override fun generateMemberPrototypes(aClass: PsiClass, originalMember: ClassMember?): Array<GenerationInfo>? {
        if (!okay) {
            return null
        }

        relevantModule?.let { relevantModule ->
            relevantModule.doPreEventGenerate(aClass, data)

            model!!.moveToLogicalPosition(position!!)

            val newMethod = relevantModule.generateEventListenerMethod(aClass, chosenClass!!, chosenName!!, data)

            if (newMethod != null) {
                val info = PsiGenerationInfo(newMethod)
                info.positionCaret(editor!!, true)
                if (method != null) {
                    info.insert(aClass, method, false)
                }

                return arrayOf(info)
            }
        }

        return null
    }

    override fun isAvailableForQuickList(editor: Editor, file: PsiFile, dataContext: DataContext): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return false

        val instance = MinecraftFacet.getInstance(module)
        return instance != null && instance.isEventGenAvailable
    }

    companion object {
        private val DUMMY_RESULT = arrayOfNulls<ClassMember>(1) //cannot return empty array, but this result won't be used anyway

        private fun isSuperEventListenerAllowed(eventClass: PsiClass, facet: MinecraftFacet): Boolean {
            val supers = eventClass.supers
            for (aSuper in supers) {
                if (facet.isEventClassValidForModule(aSuper)) {
                    return true
                }
                if (isSuperEventListenerAllowed(aSuper, facet)) {
                    return true
                }
            }
            return false
        }

        private fun isSuperEventListenerAllowed(eventClass: PsiClass, module: AbstractModule): Boolean {
            val supers = eventClass.supers
            for (aSuper in supers) {
                if (module.isEventClassValid(aSuper, null)) {
                    return true
                }
                if (isSuperEventListenerAllowed(aSuper, module)) {
                    return true
                }
            }
            return false
        }
    }
}
