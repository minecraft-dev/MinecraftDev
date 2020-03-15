/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findMethodsByInternalName
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.memberReference
import com.google.common.collect.Sets
import com.intellij.ide.projectView.impl.nodes.PsiMethodNode
import com.intellij.ide.util.AbstractTreeClassChooserDialog
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.ide.util.gotoByName.GotoSymbolModel2
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.presentation.java.ClassPresentationUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ErrorLabel
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.table.TableView
import com.intellij.util.IconUtil
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.StatusText
import java.awt.BorderLayout
import java.util.Comparator
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import org.jetbrains.annotations.Nls

class TranslationFunctionConfigurable(private val project: Project) : MasterDetailsComponent() {
    private val scopeComboBox = ComboBox(
        DefaultComboBoxModel(if (!project.isDefault) arrayOf("Global", "Project") else arrayOf("Global"))
    )

    init {
        toolbar.border = EmptyBorder(4, 7, 4, 0)
        toolbar.layout = HorizontalLayout(3, SwingConstants.CENTER)
        toolbar.add(JLabel("Scope:"))
        toolbar.add(scopeComboBox)

        scopeComboBox.addActionListener {
            reset()
        }

        initTree()
    }

    override fun reInitWholePanelIfNeeded() {
        super.reInitWholePanelIfNeeded()
        toolbar.isVisible = true
        myWholePanel.add(toolbar, BorderLayout.NORTH)
    }

    override fun reset() {
        super.reset()
        myRoot.removeAllChildren()

        val configs = if (scopeComboBox.selectedIndex == 0) {
            TranslationFunctionRepository.getGlobalConfigFiles()
        } else {
            TranslationFunctionRepository.getProjectConfigFiles(project)
        }

        for ((version, config) in configs) {
            myRoot.add(MyNode(VersionConfigurable(VersionConfiguration(
                version.versionString,
                config.inherit,
                config.entries.mapTo(mutableListOf()) { prepareFunctionForDisplay(it) }
            ))))
        }
        (myTree.model as DefaultTreeModel).reload(myRoot)
        if (myRoot.children().hasMoreElements()) {
            myTree.addSelectionRow(0)
        }
    }

    private fun prepareFunctionForDisplay(function: TranslationFunction): FunctionEntry {
        val entry = FunctionEntry(
            function.member.owner ?: "",
            function.member.name,
            function.member.descriptor ?: "",
            function.srgName,
            function.paramIndex,
            function.keyPrefix,
            function.keySuffix,
            function.formatting,
            function.foldParametersOnly
        )

        if (function.srgName) {
            val srgManager = SrgManager.findAnyInstance(project)
            srgManager?.srgMapNow?.mapToMcpMethod(function.member)?.let {
                entry.className = it.owner ?: ""
                entry.methodName = it.name
                entry.methodDescriptor = it.descriptor ?: ""
            }
        }

        return entry
    }

    private fun mapEntry(entry: FunctionEntry): TranslationFunction {
        val srgManager = SrgManager.findAnyInstance(project)
        val member = entry.member.let {
            srgManager.takeIf { entry.srgName }?.srgMapNow?.getMcpMethod(it) ?: it
        }
        return TranslationFunction(
            member,
            entry.srgName,
            entry.paramIndex,
            entry.keyPrefix,
            entry.keySuffix,
            entry.formatting,
            entry.foldParametersOnly
        )
    }

    @Nls
    override fun getDisplayName() = "Translation Functions"

    override fun getHelpTopic(): String? = null

    override fun getEmptySelectionString(): String? {
        return if (myRoot.children().hasMoreElements())
            "Select version to view"
        else
            "Please add a version and configure its functions"
    }

    override fun createActions(fromPopup: Boolean): List<AnAction> =
        listOf(
            object : DumbAwareAction("Add", "Add", IconUtil.getAddIcon()) {
                init {
                    registerCustomShortcutSet(CommonShortcuts.INSERT, myTree)
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val node = MyNode(
                        VersionConfigurable(
                            VersionConfiguration(
                                "Version Number",
                                true,
                                mutableListOf()
                            )
                        )
                    )
                    addNode(node, myRoot)
                    selectNodeInTree(node, true)
                }
            },
            MyDeleteAction()
        )

    override fun getNodeComparator(): Comparator<MyNode> {
        val fallback = super.getNodeComparator()
        return Comparator { node1, node2 ->
            val configurable1 = node1.configurable
            val configurable2 = node2.configurable
            if (configurable1 is VersionConfigurable && configurable2 is VersionConfigurable) {
                try {
                    val version1 = SemanticVersion.parse(configurable1.config.version)
                    val version2 = SemanticVersion.parse(configurable2.config.version)
                    return@Comparator version1.compareTo(version2)
                } catch (e: IllegalArgumentException) {
                }
            }
            fallback.compare(node1, node2)
        }
    }

    private class VersionConfiguration(
        var version: String,
        var inherit: Boolean,
        val functions: MutableList<FunctionEntry>
    ) {
        var initialVersion = version
        var initialInherit = inherit
    }

    private data class FunctionEntry(
        var className: String,
        var methodName: String,
        var methodDescriptor: String,
        var srgName: Boolean,
        var paramIndex: Int,
        var keyPrefix: String,
        var keySuffix: String,
        var formatting: Boolean,
        var foldParametersOnly: Boolean
    ) {
        val member: MemberReference
            get() = MemberReference(methodName, methodDescriptor, className)

        fun findClass(project: Project) =
            JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))

        fun findMethod(project: Project) = findClass(project)?.findMethods(member)?.findFirst()?.orElse(null)
    }

    private inner class VersionConfigurable(val config: VersionConfiguration) :
        NamedConfigurable<VersionConfiguration>(true, TREE_UPDATER) {
        val component: JComponent
        private val inheritCheckBox = JBCheckBox("Inherit from older versions")
        private val table = FunctionTable()
        private val classLabel = ErrorLabel("Class:")
        private val classField = ExtendableTextField(30)
        val classBrowser = TextFieldWithBrowseButton.NoPathCompletion(classField) {
            classField.text = selectClass()?.fullQualifiedName ?: return@NoPathCompletion
        }
        private val methodLabel = ErrorLabel("Method:")
        private val methodField = ExtendableTextField(20)
        private val methodBrowser = TextFieldWithBrowseButton.NoPathCompletion(methodField) {
            openMethodSelection()
        }
        private val descriptorLabel = ErrorLabel("Descriptor:")
        private val descriptorField = JBTextField()
        private val paramPanel = JPanel(BorderLayout())
        private val paramComboBox = ComboBox<String>()
        private val paramIndexField = IntegerField("parameter index", 0, Integer.MAX_VALUE)
        private val srgNameCheckBox = JBCheckBox("SRG Name")
        private val formattingCheckBox = JBCheckBox("Supports formatting (only for varargs)")
        private val foldParametersOnlyCheckBox = JBCheckBox("Fold only parameters instead of entire call")
        private val keyPrefixField = JBTextField()
        private val keySuffixField = JBTextField()
        private var updatingFields = false
        private var comboBoxActive = false

        init {
            val decorator = ToolbarDecorator.createDecorator(table)
            decorator.setRemoveAction {
                    table.listTableModel.removeRow(table.selectedRow)
                    table.clearSelection()
                }
                .setRemoveActionName("Remove function")
                .setAddAction { addNewFunction() }
                .setAddActionName("Add function")
                .disableUpDownActions()

            val builder = FormBuilder.createFormBuilder()

            inheritCheckBox.isSelected = config.inherit
            inheritCheckBox.addActionListener {
                config.inherit = inheritCheckBox.isSelected
            }
            inheritCheckBox.border = BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(0, 8),
                inheritCheckBox.border
            )
            builder.addComponent(inheritCheckBox, 5)

            val tablePanel = decorator.createPanel()
            tablePanel.border = BorderFactory.createCompoundBorder(JBUI.Borders.empty(0, 8), tablePanel.border)
            builder.addComponentFillVertically(tablePanel, 5)

            classBrowser.setButtonEnabled(true)
            classField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateSelectedEntry()
                    updateMethodBrowser()
                }
            })
            classLabel.labelFor = classBrowser
            classLabel.border = JBUI.Borders.empty(0, 10)
            classBrowser.border = JBUI.Borders.emptyRight(10)

            methodField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateSelectedEntry()
                    swapParamInput(table.selectedObject?.findMethod(project), table.selectedObject?.paramIndex)
                }
            })
            methodLabel.labelFor = methodBrowser
            methodLabel.border = JBUI.Borders.empty(0, 10)
            methodBrowser.border = JBUI.Borders.emptyRight(10)
            updateMethodBrowser()

            val descriptorPanel = JPanel(BorderLayout())
            descriptorField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateSelectedEntry()
                    swapParamInput(table.selectedObject?.findMethod(project), table.selectedObject?.paramIndex)
                }
            })
            descriptorLabel.labelFor = descriptorField
            descriptorLabel.border = JBUI.Borders.empty(0, 10)
            descriptorPanel.border = JBUI.Borders.emptyRight(10)
            descriptorPanel.add(descriptorField)

            val paramLabel = JLabel("Key Parameter:")
            paramComboBox.addActionListener { updateSelectedEntry() }
            paramIndexField.valueEditor.addListener { updateSelectedEntry() }
            paramLabel.labelFor = paramPanel
            paramLabel.border = JBUI.Borders.empty(0, 10)
            paramPanel.border = JBUI.Borders.emptyRight(10)
            paramPanel.add(paramIndexField, BorderLayout.CENTER)

            builder.addLabeledComponent(classLabel, classBrowser, 5)
            builder.addLabeledComponent(methodLabel, methodBrowser, 5)
            builder.addLabeledComponent(descriptorLabel, descriptorPanel, 5)
            builder.addLabeledComponent(paramLabel, paramPanel, 5)

            val checkBoxPanel = JPanel(HorizontalLayout(5))
            checkBoxPanel.border = JBUI.Borders.empty(0, 10)

            srgNameCheckBox.addActionListener {
                updateSelectedEntry()
            }
            formattingCheckBox.addActionListener {
                updateSelectedEntry()
            }
            foldParametersOnlyCheckBox.addActionListener {
                updateSelectedEntry()
            }

            checkBoxPanel.add(srgNameCheckBox)
            checkBoxPanel.add(formattingCheckBox)
            checkBoxPanel.add(foldParametersOnlyCheckBox)

            builder.addComponent(checkBoxPanel)

            val keyPrefixPanel = JPanel(BorderLayout())
            val keyPrefixLabel = JLabel("Key Prefix:")
            keyPrefixField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateSelectedEntry()
                }
            })
            keyPrefixLabel.labelFor = keyPrefixField
            keyPrefixLabel.border = JBUI.Borders.empty(0, 10)
            keyPrefixPanel.border = JBUI.Borders.emptyRight(10)
            keyPrefixPanel.add(keyPrefixField)

            val keySuffixPanel = JPanel(BorderLayout())
            val keySuffixLabel = JLabel("Key Suffix:")
            keySuffixField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateSelectedEntry()
                }
            })
            keySuffixLabel.labelFor = keySuffixField
            keySuffixLabel.border = JBUI.Borders.empty(0, 10, 10, 10)
            keySuffixPanel.border = JBUI.Borders.empty(0, 0, 10, 10)
            keySuffixPanel.add(keySuffixField)

            builder.addLabeledComponent(keyPrefixLabel, keyPrefixPanel, 5)
            builder.addLabeledComponent(keySuffixLabel, keySuffixPanel, 5)

            component = builder.panel

            toggleComponents(false)
            table.setModelAndUpdateColumns(ListTableModel(arrayOf(FunctionColumn()), config.functions))
            table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            table.selectionModel.addListSelectionListener {
                val selection = table.selectedObject
                toggleComponents(selection != null)
                updateFields(table.selectedObject)
            }
        }

        private fun toggleComponents(enabled: Boolean) {
            classBrowser.isEnabled = enabled
            methodBrowser.isEnabled = enabled
            descriptorField.isEnabled = enabled
            paramIndexField.isEnabled = enabled
            paramComboBox.isEnabled = enabled
            srgNameCheckBox.isEnabled = enabled
            formattingCheckBox.isEnabled = enabled
            foldParametersOnlyCheckBox.isEnabled = enabled
            keyPrefixField.isEnabled = enabled
            keySuffixField.isEnabled = enabled
        }

        private fun updateSelectedEntry() {
            if (updatingFields) {
                return
            }
            val entry = table.selectedObject ?: return
            entry.className = classField.text
            entry.methodName = methodField.text
            entry.methodDescriptor = descriptorField.text
            entry.paramIndex = if (comboBoxActive) paramComboBox.selectedIndex else paramIndexField.value
            entry.srgName = srgNameCheckBox.isSelected
            entry.formatting = formattingCheckBox.isSelected
            entry.foldParametersOnly = foldParametersOnlyCheckBox.isSelected
            entry.keyPrefix = keyPrefixField.text.trim()
            entry.keySuffix = keySuffixField.text.trim()
            validate()
            val srgManager = SrgManager.findAnyInstance(project)
            srgNameCheckBox.isEnabled = srgManager?.srgMapNow?.getMcpMethod(entry.member) == null
        }

        private fun updateFields(entry: FunctionEntry?) {
            updatingFields = true
            classField.text = entry?.className ?: ""
            methodField.text = entry?.methodName ?: ""
            descriptorField.text = entry?.methodDescriptor ?: ""
            paramIndexField.value = entry?.paramIndex ?: 0
            srgNameCheckBox.isSelected = entry?.srgName ?: false
            formattingCheckBox.isSelected = entry?.formatting ?: false
            foldParametersOnlyCheckBox.isSelected = entry?.foldParametersOnly ?: false
            keyPrefixField.text = entry?.keyPrefix ?: ""
            keySuffixField.text = entry?.keySuffix ?: ""

            val method = entry?.findMethod(project)
            swapParamInput(method, entry?.paramIndex)
            method?.also {
                val srgManager = SrgManager.findAnyInstance(project)
                srgNameCheckBox.isEnabled = srgManager?.srgMapNow?.getMcpMethod(it.memberReference) == null
                formattingCheckBox.isEnabled = it.isVarArgs
            }
            updatingFields = false
        }

        private fun swapParamInput(method: PsiMethod?, paramIndex: Int?) {
            if (method == null) {
                comboBoxActive = false
                paramPanel.remove(paramComboBox)
                paramPanel.add(paramIndexField, BorderLayout.CENTER)
            } else {
                comboBoxActive = true
                paramPanel.remove(paramIndexField)
                val params = method.parameters
                paramComboBox.model = DefaultComboBoxModel(params.map { it.name }.toTypedArray())
                paramComboBox.selectedIndex = Math.min(paramIndex ?: 0, params.size - 1)
                paramPanel.add(paramComboBox, BorderLayout.CENTER)
                paramComboBox.isEnabled = params.isNotEmpty()
            }
            paramPanel.revalidate()
            paramPanel.repaint()
        }

        private fun validate() {
            classLabel.setErrorText(null, null)
            methodLabel.setErrorText(null, null)
            descriptorLabel.setErrorText(null, null)

            val currentEntry = table.selectedObject ?: return

            val psiClass = currentEntry.findClass(project)
            val method = currentEntry.findMethod(project)
            if (psiClass != null && method == null) {
                val methods = psiClass.findMethodsByInternalName(currentEntry.methodName)
                if (methods.isEmpty()) {
                    methodLabel.setErrorText(
                        "${currentEntry.methodName} is not a method in ${currentEntry.className}",
                        JBColor.YELLOW
                    )
                } else {
                    descriptorLabel.setErrorText(
                        "There is no override of ${currentEntry.methodName} in " +
                            "${currentEntry.className} with this signature",
                        JBColor.YELLOW
                    )
                }
            }

            if (method != null && method.parameters.isEmpty()) {
                methodLabel.setErrorText(
                    "${method.name} does not take any arguments and cannot be used as translation function",
                    JBColor.RED
                )
            }

            val conflicts = config.functions.asSequence()
                .any { it !== currentEntry && it.member == currentEntry.member }
            if (conflicts) {
                methodLabel.setErrorText("A function with this method is already specified", JBColor.RED)
            }
        }

        private fun updateMethodBrowser() {
            val psiClass = table.selectedObject?.findClass(project)
            methodBrowser.setButtonEnabled(psiClass != null)
            methodBrowser.button.isVisible = psiClass != null
            swapParamInput(table.selectedObject?.findMethod(project), table.selectedObject?.paramIndex)
        }

        private fun openMethodSelection() {
            val psiClass = table.selectedObject?.findClass(project) ?: return
            val method = selectMethod(psiClass) ?: return
            methodField.text = method.internalName
            descriptorField.text = method.descriptor ?: return
        }

        private fun selectClass(): PsiClass? {
            val chooser = TreeClassChooserFactory.getInstance(project).createAllProjectScopeChooser("Choose Class")
            chooser.showDialog()
            return chooser.selected
        }

        private fun selectMethod(psiClass: PsiClass): PsiMethod? {
            val methods = (psiClass.constructors + psiClass.methods).filter { it.parameters.isNotEmpty() }
                .toTypedArray()
            val dialog = object : AbstractTreeClassChooserDialog<PsiMethod>(
                "Choose Method",
                psiClass.project,
                GlobalSearchScope.EMPTY_SCOPE,
                PsiMethod::class.java,
                null,
                null,
                null,
                false,
                false
            ) {
                override fun getClassesByName(
                    name: String,
                    checkBoxState: Boolean,
                    pattern: String,
                    searchScope: GlobalSearchScope
                ) =
                    emptyList<PsiMethod>()

                override fun getSelectedFromTreeUserObject(node: DefaultMutableTreeNode?) =
                    (node?.userObject as? PsiMethodNode)?.value

                override fun createChooseByNameModel() = MethodGotoModel(project, methods)
            }
            dialog.show()
            return dialog.selected
        }

        private fun addNewFunction() {
            val newFunction = FunctionEntry("", "", "", false, 0, "", "", false, false)
            if (!project.isDefault) {
                val psiClass = selectClass() ?: return
                val method = selectMethod(psiClass) ?: return
                newFunction.className = psiClass.fullQualifiedName ?: ""
                newFunction.methodName = method.internalName
                newFunction.methodDescriptor = method.descriptor ?: ""
                val srgManager = SrgManager.findAnyInstance(project)
                newFunction.srgName = srgManager?.srgMapNow?.getMcpMethod(method.memberReference) != null
                newFunction.foldParametersOnly = newFunction.methodName.startsWith("set")
                if (config.functions.any { it.member == newFunction.member }) {
                    Messages.showErrorDialog(
                        table,
                        "The specified method is already configured as translation function in this version!",
                        "Function already exists"
                    )
                    return
                }
            }
            table.listTableModel.addRow(newFunction)
            table.selection = setOf(newFunction)
        }

        override fun getBannerSlogan() = config.version

        override fun isModified(): Boolean {
            val reference = if (scopeComboBox.selectedIndex == 0) {
                TranslationFunctionRepository.getGlobalConfigFiles()
            } else {
                TranslationFunctionRepository.getProjectConfigFiles(project)
            }
            val initialVersion = SemanticVersion.parse(config.initialVersion)
            val referenceFunctions = reference[initialVersion]?.entries?.toSet() ?: return true
            val currentFunctions = config.functions.mapTo(mutableSetOf()) { mapEntry(it) }
            val difference = Sets.symmetricDifference(referenceFunctions, currentFunctions)

            val conflicts = config.functions.groupBy { it.member }.any { it.value.size > 1 }

            return (config.version != config.initialVersion ||
                config.inherit != config.initialInherit ||
                difference.any()) && !conflicts
        }

        override fun getDisplayName() = config.version

        override fun apply() {
            try {
                val version = SemanticVersion.parse(config.version)
                val functions = config.functions.map { mapEntry(it) }
                config.initialVersion = version.versionString
                config.initialInherit = config.inherit
                if (scopeComboBox.selectedIndex == 0) {
                    TranslationFunctionRepository.saveGlobalConfig(version, config.inherit, functions)
                } else {
                    TranslationFunctionRepository.saveProjectConfig(project, version, config.inherit, functions)
                }
            } catch (e: IllegalArgumentException) {
                throw ConfigurationException(e.message)
            }
        }

        override fun getEditableObject() = config

        override fun createOptionsPanel() = component

        override fun setDisplayName(name: String) {
            config.version = name
        }

        override fun checkName(name: String) {
            super.checkName(name)
            try {
                SemanticVersion.parse(name)
            } catch (e: IllegalArgumentException) {
                throw ConfigurationException(e.message)
            }
            val duplicateEntry = myRoot.children().asSequence()
                .mapNotNull { ((it as? MyNode)?.configurable as? VersionConfigurable)?.config }
                .any {
                    it !== config && it.version == name
                }
            if (duplicateEntry) {
                throw ConfigurationException("This version already has a configuration")
            }
        }
    }

    private inner class MethodGotoModel(project: Project, private val methods: Array<PsiMethod>) : GotoSymbolModel2(
        project,
        arrayOf(object : ChooseByNameContributor {
            override fun getItemsByName(
                name: String?,
                pattern: String?,
                project: Project?,
                includeNonProjectItems: Boolean
            ) = methods

            override fun getNames(project: Project?, includeNonProjectItems: Boolean) =
                methods.map { it.name }.toTypedArray()
        })
    ) {
        override fun getPromptText() = ""

        override fun getElementsByName(name: String, parameters: FindSymbolParameters, canceled: ProgressIndicator) =
            methods
    }

    private inner class FunctionColumn : ColumnInfo<FunctionEntry, String>("") {
        override fun valueOf(entry: FunctionEntry?): String? {
            if (entry == null) {
                return null
            }
            val method = entry.findMethod(project)
            val className = method?.containingClass?.let { ClassPresentationUtil.getNameForClass(it, false) }
                ?: entry.className
            val methodName = method?.internalName ?: entry.methodName
            val descriptor = method?.let {
                PsiFormatUtil.formatMethod(
                    it,
                    PsiSubstitutor.EMPTY,
                    PsiFormatUtilBase.SHOW_PARAMETERS,
                    PsiFormatUtilBase.SHOW_TYPE
                )
            } ?: entry.methodDescriptor
            val result = StringBuilder()
            result.append(className.trim())
            if (className.isNotBlank() && methodName != "<init>") {
                result.append('.')
            }
            if (methodName != "<init>") {
                result.append(methodName)
            }
            result.append(descriptor)
            return result.toString()
        }
    }

    private inner class FunctionTable : TableView<FunctionEntry>() {
        private val empty = object : StatusText() {
            override fun isStatusVisible(): Boolean {
                return isEmpty
            }
        }

        init {
            empty.text = "No functions defined"
            focusTraversalKeysEnabled = false
            tableHeader.isVisible = false
        }

        override fun getEmptyText(): StatusText {
            return empty
        }
    }
}
