/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.MixinModule
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.invokeDeclaredMethod
import com.demonwav.mcdev.util.invokeLater
import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase
import com.intellij.codeInsight.generation.GenerationInfo
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.impl.CreateClassDialog
import com.intellij.ide.util.ChooseElementsDialog
import com.intellij.ide.util.EditorHelper
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBCheckBox
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.IncorrectOperationException
import java.awt.event.ItemEvent
import javax.swing.JComponent
import org.jetbrains.java.generate.exception.GenerateCodeException

class GenerateAccessorHandler : GenerateMembersHandlerBase("Generate Accessor/Invoker") {

    private val log =
        Logger.getInstance("#com.demonwav.mcdev.platform.mixin.action.GenerateAccessorHandler")

    private var generateGetters = false
    private var generateSetters = false

    // Because "invoke" in the superclass is final, it cannot be overridden directly
    fun customInvoke(
        project: Project,
        editor: Editor,
        file: PsiFile
    ) {
        val aClass = OverrideImplementUtil.getContextClass(project, editor, file, false)
        if (aClass == null || aClass.isInterface) {
            return // ?
        }
        log.assertTrue(aClass.isValid)
        log.assertTrue(aClass.containingFile != null)
        try {
            val members = chooseOriginalMembers(aClass, project, editor) ?: return

            val mixinClass = getOrCreateAccessorMixin(project, aClass) ?: return
            val mixinEditor = EditorHelper.openInEditor(mixinClass)

            if (!EditorModificationUtil.checkModificationAllowed(mixinEditor)) {
                return
            }
            if (!FileDocumentManager.getInstance().requestWriting(mixinEditor.document, project)) {
                return
            }

            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    val offset = mixinEditor.caretModel.offset
                    try {
                        this.invokeDeclaredMethod(
                            "doGenerate",
                            params = arrayOf(
                                Project::class.java,
                                Editor::class.java,
                                PsiClass::class.java,
                                Array<ClassMember>::class.java
                            ),
                            args = arrayOf(
                                project,
                                mixinEditor,
                                mixinClass,
                                members
                            ),
                            owner = GenerateMembersHandlerBase::class.java
                        )
                    } catch (e: GenerateCodeException) {
                        val message = e.message ?: "Unknown error"
                        ApplicationManager.getApplication().invokeLater(
                            {
                                if (!mixinEditor.isDisposed) {
                                    mixinEditor.caretModel.moveToOffset(offset)
                                    HintManager.getInstance().showErrorHint(editor, message)
                                }
                            },
                            project.disposed
                        )
                    }
                },
                null,
                null
            )
        } finally {
            cleanup()
        }
    }

    override fun hasMembers(aClass: PsiClass): Boolean {
        if (aClass.isMixin) {
            return false
        }

        if (aClass.fields.any { canHaveAccessor(it) }) {
            return true
        }
        if (aClass.methods.any { canHaveInvoker(it) }) {
            return true
        }

        return false
    }

    override fun getAllOriginalMembers(aClass: PsiClass?): Array<ClassMember> {
        if (aClass == null) {
            return ClassMember.EMPTY_ARRAY
        }

        val members = mutableListOf<ClassMember>()
        members.addAll(
            aClass.fields
                .filter { canHaveAccessor(it) }
                .map { PsiFieldMember(it) }
        )
        members.addAll(
            aClass.methods
                .filter { canHaveInvoker(it) }
                .map { PsiMethodMember(it) }
        )
        return members.toTypedArray()
    }

    override fun chooseOriginalMembers(aClass: PsiClass?, project: Project?, editor: Editor?): Array<ClassMember>? {
        project ?: return null

        val offset = editor?.caretModel?.offset ?: return null
        val element = aClass?.containingFile?.findElementAt(offset) ?: return null
        val targetClass = element.findContainingClass() ?: return null
        val candidates = getAllOriginalMembers(targetClass)

        val headerPanel = HeaderPanel()
        val chooser = MemberChooser(candidates, false, true, project, headerPanel, arrayOf())
        chooser.selectElements(
            candidates.filter { candidate ->
                if (candidate !is PsiElementClassMember<*>) {
                    return@filter false
                }
                val range = candidate.element.textRange
                return@filter range != null && range.contains(offset)
            }.toTypedArray()
        )

        if (!chooser.showAndGet()) {
            return null
        }
        val selectedMembers = chooser.selectedElements
        if (selectedMembers.isNullOrEmpty()) {
            return null
        }

        generateGetters = headerPanel.gettersCheckbox.isSelected
        generateSetters = headerPanel.settersCheckbox.isSelected

        return selectedMembers.toTypedArray()
    }

    private fun canHaveAccessor(field: PsiField): Boolean {
        val isPublic = field.modifierList?.hasExplicitModifier(PsiModifier.PUBLIC) == true
        val isFinal = field.modifierList?.hasExplicitModifier(PsiModifier.FINAL) == true
        val isEnumConstant = field is PsiEnumConstant
        return (!isPublic || isFinal) && !isEnumConstant
    }

    private fun canHaveInvoker(method: PsiMethod): Boolean {
        return !method.modifierList.hasExplicitModifier(PsiModifier.PUBLIC)
    }

    private fun getOrCreateAccessorMixin(project: Project, targetClass: PsiClass): PsiClass? {
        val targetInternalName = targetClass.fullQualifiedName?.replace('.', '/') ?: return null
        val mixins = MixinModule.getAllMixinClasses(project, GlobalSearchScope.projectScope(project))
            .asSequence()
            .filter { it.isWritable }
            .filter { it.isAccessorMixin }
            .filter { it.mixinTargets.any { target -> target.name == targetInternalName } }
            .toList()

        return when (mixins.size) {
            0 -> createAccessorMixin(project, targetClass)
            1 -> mixins[0]
            else -> chooseAccessorMixin(project, mixins)
        }
    }

    private fun createAccessorMixin(project: Project, targetClass: PsiClass): PsiClass? {
        val config = MixinModule
            .getMixinConfigs(project, GlobalSearchScope.projectScope(project))
            .maxByOrNull {
                countAccessorMixins(project, it.qualifiedMixins) +
                    countAccessorMixins(project, it.qualifiedClient) +
                    countAccessorMixins(project, it.qualifiedServer)
            }

        if (config == null) {
            // TODO: generate the mixin configuration file (modding platform dependent)
            val message = "There is no matching Mixin configuration file found in the project. " +
                "Please create one and try again."
            Messages.showInfoMessage(project, message, "Generate Accessor/Invoker")
            return null
        }

        val defaultPkg = config.pkg ?: ""
        val defaultName = "${targetClass.name}Accessor"
        val defaultModule = config.file?.let { ModuleUtil.findModuleForFile(it, project) }

        val dialog = CreateClassDialog(
            project,
            "Create Accessor Mixin",
            defaultName,
            defaultPkg,
            CreateClassKind.CLASS,
            true,
            defaultModule
        )
        if (!dialog.showAndGet()) {
            return null
        }
        val pkg = dialog.targetDirectory ?: return null
        val name = dialog.className

        return WriteCommandAction.writeCommandAction(project)
            .withName("Generate Accessor/Invoker")
            .withGroupId("Generate Accessor/Invoker")
            .compute<PsiClass, RuntimeException> {
                IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace()
                val clazz = try {
                    JavaDirectoryService.getInstance().createInterface(pkg, name)
                } catch (e: IncorrectOperationException) {
                    invokeLater {
                        val message = MCDevBundle.message(
                            "intention.error.cannot.create.class.message",
                            name,
                            e.localizedMessage
                        )
                        Messages.showErrorDialog(
                            project,
                            message,
                            MCDevBundle.message("intention.error.cannot.create.class.title")
                        )
                    }
                    return@compute null
                }
                val factory = JavaPsiFacade.getElementFactory(project)
                val targetAccessible = targetClass.modifierList?.hasExplicitModifier(PsiModifier.PUBLIC) == true &&
                    targetClass.name != null
                val annotationText = if (targetAccessible) {
                    "@${MixinConstants.Annotations.MIXIN}(${targetClass.qualifiedName}.class)"
                } else {
                    "@${MixinConstants.Annotations.MIXIN}(targets=\"${targetClass.fullQualifiedName}\")"
                }
                val annotation = factory.createAnnotationFromText(annotationText, clazz)
                AddAnnotationFix(MixinConstants.Annotations.MIXIN, clazz, annotation.parameterList.attributes)
                    .applyFix()

                val module = clazz.findModule() ?: return@compute null
                val configToWrite = MixinModule.getBestWritableConfigForMixinClass(
                    project,
                    GlobalSearchScope.moduleScope(module),
                    clazz.fullQualifiedName ?: ""
                )
                configToWrite?.qualifiedMixins?.add(clazz.fullQualifiedName)

                return@compute clazz
            }
    }

    private fun countAccessorMixins(project: Project, names: List<String?>): Int {
        return names.asSequence()
            .filterNotNull()
            .map { it.replace('$', '.') }
            .distinct()
            .flatMap {
                JavaPsiFacade.getInstance(project).findClasses(it, GlobalSearchScope.projectScope(project)).asSequence()
            }
            .filter { it.isAccessorMixin }
            .count()
    }

    private fun chooseAccessorMixin(project: Project, mixins: List<PsiClass>): PsiClass? {
        val title = "Choose Accessor Mixin"
        val description = "Select an Accessor Mixin to generate the accessor members in"
        val chooser = object : ChooseElementsDialog<PsiClass>(project, mixins, title, description) {
            init {
                myChooser.setSingleSelectionMode()
            }

            override fun getItemIcon(item: PsiClass?) = PlatformAssets.MIXIN_ICON

            override fun getItemText(item: PsiClass): String {
                // keep adding packages from the full qualified name until our name is unique
                @Suppress("DialogTitleCapitalization")
                val parts = item.qualifiedName?.split(".") ?: return "null"
                var name = ""
                for (part in parts.asReversed()) {
                    name = if (name.isEmpty()) {
                        part
                    } else {
                        "$part.$name"
                    }
                    if (mixins.none { it !== item && it.qualifiedName?.endsWith(".$name") == true }) {
                        return name
                    }
                }
                return name
            }
        }
        val result = chooser.showAndGetResult()
        return if (result.size == 1) result[0] else null
    }

    override fun generateMemberPrototypes(aClass: PsiClass, originalMember: ClassMember): Array<GenerationInfo> {
        return when (originalMember) {
            is PsiFieldMember -> {
                val accessors = generateAccessors(
                    aClass.project,
                    originalMember.element,
                    aClass,
                    generateGetters,
                    generateSetters
                )
                OverrideImplementUtil.convert2GenerationInfos(accessors).toTypedArray()
            }
            is PsiMethodMember -> {
                val invoker = generateInvoker(
                    aClass.project,
                    originalMember.element,
                    aClass
                ) ?: return GenerationInfo.EMPTY_ARRAY
                arrayOf(OverrideImplementUtil.createGenerationInfo(invoker))
            }
            else -> GenerationInfo.EMPTY_ARRAY
        }
    }

    private fun generateAccessors(
        project: Project,
        target: PsiField,
        mixin: PsiClass,
        generateGetter: Boolean,
        generateSetter: Boolean
    ): List<PsiMethod> {
        val factory = JavaPsiFacade.getElementFactory(project)

        val isStatic = target.modifierList?.hasExplicitModifier(PsiModifier.STATIC) == true

        val accessors = arrayListOf<PsiMethod>()

        if (generateGetter) {
            val prefix = if (target.type == PsiType.BOOLEAN) "is" else "get"
            val method = factory.createMethodFromText(
                """
                @${MixinConstants.Annotations.ACCESSOR}
                ${staticPrefix(isStatic)}ReturnType $prefix${target.name.capitalize()}()${methodBody(isStatic)}
                """.trimIndent(),
                mixin
            )
            target.typeElement?.let { method.returnTypeElement?.replace(it) }
            accessors.add(method)
        }
        if (generateSetter) {
            val method = factory.createMethodFromText(
                "@${MixinConstants.Annotations.ACCESSOR}\n" +
                    staticPrefix(isStatic) + "void set${target.name.capitalize()}" +
                    "(ParamType ${target.name})" + methodBody(isStatic),
                mixin
            )
            target.typeElement?.let { method.parameterList.parameters[0].typeElement?.replace(it) }
            if (target.modifierList?.hasExplicitModifier(PsiModifier.FINAL) == true) {
                AddAnnotationFix(MixinConstants.Annotations.MUTABLE, method).applyFix()
            }
            accessors.add(method)
        }

        return accessors
    }

    private fun generateInvoker(project: Project, target: PsiMethod, mixin: PsiClass): PsiMethod? {
        val factory = JavaPsiFacade.getElementFactory(project)

        val isStatic = target.modifierList.hasExplicitModifier(PsiModifier.STATIC) || target.isConstructor
        val name = if (target.isConstructor) {
            "create${target.containingClass?.name?.capitalize()}"
        } else {
            "call${target.name.capitalize()}"
        }
        val invokerParams = if (target.isConstructor) {
            "(\"<init>\")"
        } else {
            ""
        }

        val method = factory.createMethodFromText(
            """
            @${MixinConstants.Annotations.INVOKER}$invokerParams
            ${staticPrefix(isStatic)}ReturnType $name()${methodBody(isStatic)}
            """.trimIndent(),
            mixin
        )
        if (target.isConstructor) {
            val targetClass = target.containingClass ?: return null
            method.returnTypeElement?.replace(factory.createTypeElement(factory.createType(targetClass)))
        } else {
            target.returnTypeElement?.let { method.returnTypeElement?.replace(it) }
        }
        method.parameterList.replace(target.parameterList)
        method.throwsList.replace(target.throwsList)

        return method
    }

    private fun staticPrefix(isStatic: Boolean): String {
        return if (isStatic) {
            "static "
        } else {
            ""
        }
    }

    private fun methodBody(isStatic: Boolean): String {
        return if (isStatic) {
            " { throw new java.lang.UnsupportedOperationException(); }"
        } else {
            ";"
        }
    }

    private class HeaderPanel : JComponent() {
        val gettersCheckbox = JBCheckBox("Generate getter accessors")
        val settersCheckbox = JBCheckBox("Generate setter accessors")

        init {
            gettersCheckbox.isSelected = true
            gettersCheckbox.addItemListener {
                if (it.stateChange == ItemEvent.DESELECTED) {
                    settersCheckbox.isSelected = true
                }
            }
            settersCheckbox.addItemListener {
                if (it.stateChange == ItemEvent.DESELECTED) {
                    gettersCheckbox.isSelected = true
                }
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
