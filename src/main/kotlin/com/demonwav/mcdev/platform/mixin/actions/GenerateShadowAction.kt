package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.nameAndDescriptor
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

class GenerateShadowAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = getClassOfElement(file.findElementAt(offset)) ?: return

        val fields = (findShadowableFields(psiClass) ?: return)
                .map(::PsiFieldMember).toTypedArray()

        if (fields.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No fields to shadow have been found")
            return
        }

        val chooser = MemberChooser<PsiFieldMember>(fields, false, true, project)
        chooser.title = "Select Fields to Shadow"
        chooser.setCopyJavadocVisible(false)
        chooser.show()

        val elements = chooser.selectedElements ?: return
        if (elements.isEmpty()) {
            return
        }

        runWriteAction {
            GenerateMembersUtil.insertMembersAtOffset(file, offset, elements.map {
                val shadowField = shadowField(project, it.element)
                shadowField.modifierList!!.addAnnotation(MixinConstants.Annotations.SHADOW)

                PsiGenerationInfo(shadowField)
            })
                    // Select first element in editor
                    .first().positionCaret(editor, false)
        }
    }

    private fun shadowField(project: Project, field: PsiField): PsiField {
        val factory = JavaPsiFacade.getElementFactory(project)

        val fieldModifiers = field.modifierList!!
        val shadow = factory.createField(field.name!!, field.type)
        val shadowModifiers = shadow.modifierList!!

        // Copy annotations
        for (annotation in fieldModifiers.annotations) {
            shadowModifiers.addAfter(factory.createAnnotationFromText(annotation.text, field), null)
        }

        VisibilityUtil.setVisibility(shadowModifiers, VisibilityUtil.getVisibilityModifier(fieldModifiers))

        return shadow
    }

    private fun findShadowableFields(psiClass: PsiClass): Stream<PsiField>? {
        val targets = MixinUtils.getAllMixedClasses(psiClass).values
        return when (targets.size) {
            0 -> null
            1 -> targets.single().fields.stream()
            else -> targets.stream()
                    .flatMap { target -> target.fields.stream() }
                    .collect(Collectors.groupingBy(PsiField::nameAndDescriptor))
                    .values.stream()
                    .filter { it.size >= targets.size }
                    .map { it.first() }
        }?.filter {
            // Filter fields which are already in the Mixin class
            psiClass.findFieldByName(it.name, false) == null
        }
    }

}
