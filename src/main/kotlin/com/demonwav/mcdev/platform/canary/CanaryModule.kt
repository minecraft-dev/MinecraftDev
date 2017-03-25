/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.canary.generation.CanaryGenerationData
import com.demonwav.mcdev.platform.canary.util.CanaryConstants
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

class CanaryModule<out T : AbstractModuleType<*>>(facet: MinecraftFacet, override val moduleType: T) : AbstractModule(facet) {

    override val type: PlatformType = moduleType.platformType
    private var canaryInf: VirtualFile? = null

    init {
        setup()
    }

    private fun setup() {
        canaryInf = facet.findFile(CanaryConstants.CANARY_INF, SourceType.RESOURCE)
    }

    fun getCanaryInf(): VirtualFile? {
        if (canaryInf == null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            setup()
        }
        return canaryInf
    }

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        CanaryConstants.HOOK_CLASS == eventClass.qualifiedName

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not a subclass of ${CanaryConstants.HOOK_CLASS}\n" +
        "Compiling and running this listener may result in a runtime exception"

    override fun generateEventListenerMethod(containingClass: PsiClass,
                                             chosenClass: PsiClass,
                                             chosenName: String,
                                             data: GenerationData?): PsiMethod? {

        val canaryData = data as CanaryGenerationData

        val method = generateCanaryStyleEventListenerMethod(
            chosenClass,
            chosenName,
            project,
            CanaryConstants.HOOK_HANDLER_ANNOTATION,
            canaryData.isIgnoreCanceled
        )

        if (canaryData.priority != "NORMAL") {
            val list = method.modifierList
            val annotation = list.findAnnotation(CanaryConstants.HOOK_HANDLER_ANNOTATION) ?: return method

            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(CanaryConstants.PRIORITY_CLASS + "." + canaryData.priority, annotation)

            annotation.setDeclaredAttributeValue("priority", value)
        }

        return method
    }

    override fun dispose() {
        super.dispose()

        canaryInf = null
    }

    companion object {
        fun generateCanaryStyleEventListenerMethod(chosenClass: PsiClass,
                                                   chosenName: String,
                                                   project: Project,
                                                   annotationName: String,
                                                   setIgnoreCancelled: Boolean): PsiMethod {
            val newMethod = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID)

            val list = newMethod.parameterList
            val parameter = JavaPsiFacade.getElementFactory(project)
                .createParameter(
                    "event",
                    PsiClassType.getTypeByName(chosenClass.qualifiedName, project, GlobalSearchScope.allScope(project))
                )
            list.add(parameter)

            val modifierList = newMethod.modifierList
            val annotation = modifierList.addAnnotation(annotationName)

            if (setIgnoreCancelled) {
                val value = JavaPsiFacade.getElementFactory(project).createExpressionFromText("true", annotation)
                annotation.setDeclaredAttributeValue<PsiAnnotationMemberValue>("ignoreCanceled", value)
            }

            return newMethod
        }
    }
}
