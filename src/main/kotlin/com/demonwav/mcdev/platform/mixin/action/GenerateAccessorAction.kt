/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.MixinModule
import com.demonwav.mcdev.platform.mixin.util.MCAddMethodFix
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.invokeLater
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.impl.CreateClassDialog
import com.intellij.ide.util.ChooseElementsDialog
import com.intellij.ide.util.EditorHelper
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBCheckBox
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.IncorrectOperationException
import java.awt.event.ItemEvent
import javax.swing.JComponent

class GenerateAccessorAction : NonMixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        val targetClass = element.findContainingClass() ?: return
        val candidates = mutableListOf<ClassMember>()
        candidates.addAll(targetClass.fields.filter { it.modifierList?.hasExplicitModifier(PsiModifier.PUBLIC) != true }.map { PsiFieldMember(it) })
        candidates.addAll(targetClass.methods.filter { !it.modifierList.hasExplicitModifier(PsiModifier.PUBLIC) }.map { PsiMethodMember(it) })

        val headerPanel = HeaderPanel()
        val chooser = MemberChooser(candidates.toTypedArray(), false, true, project, headerPanel, arrayOf())
        chooser.selectElements(candidates.filter { candidate ->
            if (candidate !is PsiElementClassMember<*>)
                return@filter false
            val range = candidate.element.textRange
            return@filter range != null && range.contains(offset)
        }.toTypedArray())

        invokeLater {
            if (!chooser.showAndGet())
                return@invokeLater
            val selectedMembers = chooser.selectedElements
            if (selectedMembers.isNullOrEmpty())
                return@invokeLater
            val generateGetters = headerPanel.gettersCheckbox.isSelected
            val generateSetters = headerPanel.settersCheckbox.isSelected

            val mixin = getOrCreateAccessorMixin(project, targetClass) ?: return@invokeLater

            WriteCommandAction.writeCommandAction(project).withName("Generate Accessor/Invoker").withGroupId("Generate Accessor/Invoker").run<RuntimeException> {
                IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace()

                for (member in selectedMembers) {
                    @Suppress("MoveVariableDeclarationIntoWhen") val elem = (member as? PsiElementClassMember<*>)?.element ?: continue
                    when (elem) {
                        is PsiField -> generateAccessor(project, elem, mixin, generateGetters, generateSetters)
                        is PsiMethod -> generateInvoker(project, elem, mixin)
                    }
                }
            }

            EditorHelper.openInEditor(mixin)
        }
    }

    private fun getOrCreateAccessorMixin(project: Project, targetClass: PsiClass): PsiClass? {
        val mixins = MixinModule.getAllMixins(project, GlobalSearchScope.projectScope(project)).
                asSequence().
                filter { it.isWritable }.
                filter { it.isAccessorMixin }.
                filter { it.mixinTargets.any { target -> target.qualifiedName == targetClass.qualifiedName } }.
                toList()

        return when (mixins.size) {
            0 -> createAccessorMixin(project, targetClass)
            1 -> mixins[0]
            else -> chooseAccessorMixin(project, mixins)
        }
    }

    private fun createAccessorMixin(project: Project, targetClass: PsiClass): PsiClass? {
        val config = MixinModule.getMixinConfigs(project, GlobalSearchScope.projectScope(project)).maxBy {
            return@maxBy countAccessorMixins(project, it.qualifiedMixins) +
                countAccessorMixins(project, it.qualifiedClient) +
                countAccessorMixins(project, it.qualifiedServer)
        } ?: return null

        val defaultPkg = config.pkg ?: ""
        val defaultName = "${targetClass.name}Accessor"
        val defaultModule = config.file?.let { ModuleUtil.findModuleForFile(it, project) }

        val dialog = CreateClassDialog(project, "Create Accessor Mixin", defaultName, defaultPkg, CreateClassKind.CLASS, true, defaultModule)
        if (!dialog.showAndGet())
            return null
        val pkg = dialog.targetDirectory ?: return null
        val name = dialog.className

        var clazz: PsiClass? = null
        WriteCommandAction.writeCommandAction(project).withName("Generate Accessor/Invoker").withGroupId("Generate Accessor/Invoker").run<RuntimeException> {
            IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace()
            try {
                clazz = JavaDirectoryService.getInstance().createInterface(pkg, name)
            } catch (e: IncorrectOperationException) {
                invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "${CodeInsightBundle.message("intention.error.cannot.create.class.message", name)}\n${e.localizedMessage}",
                        CodeInsightBundle.message("intention.error.cannot.create.class.title")
                    )
                }
                return@run
            }
            val factory = JavaPsiFacade.getElementFactory(project)
            val annotationText = if (targetClass.modifierList?.hasExplicitModifier(PsiModifier.PUBLIC) == true && targetClass.name != null)
                "@${MixinConstants.Annotations.MIXIN}(${targetClass.qualifiedName}.class)"
            else
                "@${MixinConstants.Annotations.MIXIN}(targets=\"${targetClass.fullQualifiedName}\")"
            val annotation = factory.createAnnotationFromText(annotationText, clazz)
            AddAnnotationFix(MixinConstants.Annotations.MIXIN, clazz!!, annotation.parameterList.attributes).applyFix()

            val module = clazz!!.findModule() ?: return@run
            MixinModule.getBestWritableConfigForMixinClass(project, GlobalSearchScope.moduleScope(module), clazz!!.qualifiedName ?: "")?.
                    qualifiedMixins?.
                    add(clazz!!.qualifiedName)
        }

        return clazz
    }

    private fun countAccessorMixins(project: Project, names: List<String?>): Int {
        return names.asSequence().
                filterNotNull().
                distinct().
                flatMap { JavaPsiFacade.getInstance(project).findClasses(it, GlobalSearchScope.projectScope(project)).asSequence() }.
                filter { it.isAccessorMixin }.
                count()
    }

    private fun chooseAccessorMixin(project: Project, mixins: List<PsiClass>): PsiClass? {
        val chooser = object : ChooseElementsDialog<PsiClass>(project, mixins, "Choose Accessor Mixin", "Select an Accessor Mixin to generate the accessor members in") {
            init {
                myChooser.setSingleSelectionMode()
            }

            override fun getItemIcon(item: PsiClass?) = PlatformAssets.MIXIN_ICON

            override fun getItemText(item: PsiClass): String {
                // keep adding packages from the full qualified name until our name is unique
                val parts = item.qualifiedName?.split(".") ?: return "null"
                var name = ""
                for (part in parts.asReversed()) {
                    name = if (name.isEmpty()) part else "$part.$name"
                    if (mixins.none { it !== item && it.qualifiedName?.endsWith(".$name") == true })
                        return name
                }
                return name
            }
        }
        val result = chooser.showAndGetResult()
        return if (result.size == 1) result[0] else null
    }

    private fun generateAccessor(project: Project, target: PsiField, mixin: PsiClass, generateGetter: Boolean, generateSetter: Boolean) {
        val factory = JavaPsiFacade.getElementFactory(project)

        val isStatic = target.modifierList?.hasExplicitModifier(PsiModifier.STATIC) == true
        if (generateGetter) {
            val prefix = if (target.type == PsiType.BOOLEAN) "is" else "get"
            val method = factory.createMethodFromText("""
                @${MixinConstants.Annotations.ACCESSOR}
                ${staticPrefix(isStatic)}ReturnType $prefix${target.name.capitalize()}()${methodBody(isStatic)}
                """.trimIndent(), mixin)
            target.typeElement?.let { method.returnTypeElement?.replace(it) }
            MCAddMethodFix(method, mixin).applyFix()
        }
        if (generateSetter) {
            val method = factory.createMethodFromText("""
                @${MixinConstants.Annotations.ACCESSOR}
                ${staticPrefix(isStatic)}void set${target.name.capitalize()}(ParamType ${target.name})${methodBody(isStatic)}
            """.trimIndent(), mixin)
            target.typeElement?.let { method.parameterList.parameters[0].typeElement?.replace(it) }
            if (target.modifierList?.hasExplicitModifier(PsiModifier.FINAL) == true) {
                AddAnnotationFix(MixinConstants.Annotations.MUTABLE, method).applyFix()
            }
            MCAddMethodFix(method, mixin).applyFix()
        }
    }

    private fun generateInvoker(project: Project, target: PsiMethod, mixin: PsiClass) {
        val factory = JavaPsiFacade.getElementFactory(project)

        val isStatic = target.modifierList.hasExplicitModifier(PsiModifier.STATIC) || target.isConstructor
        val name = if (target.isConstructor)
            "create${target.containingClass?.name?.capitalize()}"
        else
            "call${target.name.capitalize()}"

        val method = factory.createMethodFromText("""
            @${MixinConstants.Annotations.INVOKER}
            ${staticPrefix(isStatic)}ReturnType $name()${methodBody(isStatic)}
        """.trimIndent(), mixin)
        if (target.isConstructor) {
            val targetClass = target.containingClass ?: return
            method.returnTypeElement?.replace(factory.createTypeElement(factory.createType(targetClass)))
        } else {
            target.returnTypeElement?.let { method.returnTypeElement?.replace(it) }
        }
        method.parameterList.replace(target.parameterList)
        method.throwsList.replace(target.throwsList)

        MCAddMethodFix(method, mixin).applyFix()
    }

    private fun staticPrefix(isStatic: Boolean): String {
        return if (isStatic)
            "static "
        else
            ""
    }

    private fun methodBody(isStatic: Boolean): String {
        return if (isStatic)
            "{ throw new java.lang.UnsupportedOperationException(); }"
        else
            ";"
    }

    private class HeaderPanel : JComponent() {
        val gettersCheckbox = JBCheckBox("Generate Getter Accessors")
        val settersCheckbox = JBCheckBox("Generate Setter Accessors")
        init {
            gettersCheckbox.isSelected = true
            gettersCheckbox.addItemListener {
                if (it.stateChange == ItemEvent.DESELECTED)
                    settersCheckbox.isSelected = true
            }
            settersCheckbox.addItemListener {
                if (it.stateChange == ItemEvent.DESELECTED)
                    gettersCheckbox.isSelected = true
            }
            layout = GridLayoutManager(2, 1)
            add(gettersCheckbox, createConstraints(0))
            add(settersCheckbox, createConstraints(1))
        }
        private fun createConstraints(row: Int): GridConstraints {
            val constraints = GridConstraints()
            constraints.anchor = GridConstraints.ANCHOR_WEST
            constraints.row = row
            return constraints
        }
    }

}
