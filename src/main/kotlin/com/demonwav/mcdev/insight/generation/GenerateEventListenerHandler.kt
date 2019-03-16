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

    private data class GenerateData(
         var editor: Editor,
         var position: LogicalPosition,
         var method: PsiMethod?,
         var model: CaretModel,
         var data: GenerationData?,
         var chosenClass: PsiClass,
         var chosenName: String,
         var relevantModule: AbstractModule
    )

    private var data: GenerateData? = null

    override fun getHelpId() = "Generate Event Listener Dialog"

    override fun chooseOriginalMembers(aClass: PsiClass, project: Project, editor: Editor): Array<ClassMember>? {
        val moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return null

        val facet = MinecraftFacet.getInstance(moduleForPsiElement) ?: return null

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(RefactoringBundle.message("choose.destination.class"),
                                                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(moduleForPsiElement, false),
                                                { aClass1 -> isSuperEventListenerAllowed(aClass1, facet) }, null
            )

        chooser.showDialog()
        val chosenClass = chooser.selected ?: return null

        val relevantModule = facet.modules.asSequence()
            .filter { m -> isSuperEventListenerAllowed(chosenClass, m) }
            .firstOrNull() ?: return null

        val chosenClassName = chosenClass.nameIdentifier?.text ?: return null

        val generationDialog = EventGenerationDialog(
            editor,
            relevantModule.moduleType.getEventGenerationPanel(chosenClass),
            chosenClassName,
            relevantModule.moduleType.getDefaultListenerName(chosenClass)
        )

        val okay = generationDialog.showAndGet()

        if (!okay) {
            return null
        }

        val dialogDAta = generationDialog.data
        val chosenName = generationDialog.chosenName

        val position = editor.caretModel.logicalPosition

        val method = PsiTreeUtil.getParentOfType(aClass.containingFile.findElementAt(editor.caretModel.offset), PsiMethod::class.java)

        this.data = GenerateData(editor, position, method, editor.caretModel, dialogDAta, chosenClass, chosenName, relevantModule)

        return DUMMY_RESULT
    }

    override fun getAllOriginalMembers(aClass: PsiClass) = null

    override fun generateMemberPrototypes(aClass: PsiClass, originalMember: ClassMember?): Array<GenerationInfo>? {
        if (data == null) {
            return null
        }

        data?.let { data ->
            data.relevantModule.doPreEventGenerate(aClass, data.data)

            data.model.moveToLogicalPosition(data.position)

            val newMethod = data.relevantModule.generateEventListenerMethod(aClass, data.chosenClass, data.chosenName, data.data)

            if (newMethod != null) {
                val info = PsiGenerationInfo(newMethod)
                info.positionCaret(data.editor, true)
                if (data.method != null) {
                    info.insert(aClass, data.method, false)
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

    companion object {
        private val DUMMY_RESULT = arrayOfNulls<ClassMember>(1).castNotNull() //cannot return empty array, but this result won't be used anyway
    }
}
