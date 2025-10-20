package com.demonwav.mcdev.platform.mcp.aw.quickfix

import com.demonwav.mcdev.platform.mcp.aw.config.IgnoredClassNamesConfig
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwClassName
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls

class IgnoreClassWarningFix(private val className: String, private val element: PsiElement) : IntentionAction {

    override fun getText() = "Ignore warnings for '$className'"

    override fun getFamilyName() = "Ignore warnings"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = element.isValid

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val config = IgnoredClassNamesConfig.getInstance(project)
        val ignored = config.ignoredClassNames.toMutableSet()
        ignored.add(className)
        config.ignoredClassNames = ignored

        file?.let {
            DaemonCodeAnalyzer.getInstance(project).restart(it)
        }
    }

    override fun startInWriteAction(): Boolean = false
}