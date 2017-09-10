/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.generation.BukkitGenerationData
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.findContainingMethod
import com.google.common.base.Objects
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.annotations.Contract
import java.util.Arrays

class BukkitModule<T : AbstractModuleType<*>> constructor(facet: MinecraftFacet, type: T) : AbstractModule(facet) {

    private var pluginYml: VirtualFile? = null

    override lateinit var type: PlatformType
        private set

    override lateinit var moduleType: T
        private set

    init {
        this.moduleType = type
        this.type = type.platformType
        setup()
    }

    private fun setup() {
        pluginYml = facet.findFile("plugin.yml", SourceType.RESOURCE)
    }

    fun getPluginYml(): VirtualFile? {
        if (pluginYml == null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            setup()
        }
        return pluginYml
    }

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) =
        BukkitConstants.EVENT_CLASS == eventClass.qualifiedName

    override fun isStaticListenerSupported(method: PsiMethod) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) =
        "Parameter is not a subclass of org.bukkit.event.Event\n" +
        "Compiling and running this listener may result in a runtime exception"

    override fun doPreEventGenerate(psiClass: PsiClass, data: GenerationData?) {
        if (!psiClass.extendsOrImplements(BukkitConstants.LISTENER_CLASS)) {
            psiClass.addImplements(BukkitConstants.LISTENER_CLASS)
        }
    }

    override fun generateEventListenerMethod(containingClass: PsiClass,
                                             chosenClass: PsiClass,
                                             chosenName: String,
                                             data: GenerationData?): PsiMethod? {
        val bukkitData = data as BukkitGenerationData

        val method = generateBukkitStyleEventListenerMethod(
            chosenClass,
            chosenName,
            project,
            BukkitConstants.HANDLER_ANNOTATION,
            bukkitData.isIgnoreCanceled
        )

        if (bukkitData.eventPriority != "NORMAL") {
            val list = method.modifierList
            val annotation = list.findAnnotation(BukkitConstants.HANDLER_ANNOTATION) ?: return method

            val value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(BukkitConstants.EVENT_PRIORITY_CLASS + "." + bukkitData.eventPriority, annotation)

            annotation.setDeclaredAttributeValue("priority", value)
        }

        return method
    }

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? {
        val method = expression.findContainingMethod() ?: return null

        val annotation = method.modifierList.findAnnotation(BukkitConstants.HANDLER_ANNOTATION) ?: return null

        // We are in an event method
        val annotationMemberValue = annotation.findAttributeValue("ignoreCancelled") as? PsiLiteralExpression ?: return null

        val value = annotationMemberValue
        if (value.value !is Boolean) {
            return null
        }

        val ignoreCancelled = (value.value as Boolean?)!!

        // If we aren't ignoring cancelled then any check for event being cancelled is valid
        if (!ignoreCancelled) {
            return null
        }

        val methodExpression = expression.methodExpression
        val qualifierExpression = methodExpression.qualifierExpression
        val resolve = methodExpression.resolve()

        if (qualifierExpression == null) {
            return null
        }
        if (resolve == null) {
            return null
        }

        if (standardSkip(method, qualifierExpression)) {
            return null
        }

        val context = resolve.context as? PsiClass ?: return null

        if (!context.extendsOrImplements(BukkitConstants.CANCELLABLE_CLASS)) {
            return null
        }

        if (resolve !is PsiMethod) {
            return null
        }

        if (resolve.name != BukkitConstants.EVENT_ISCANCELLED_METHOD_NAME) {
            return null
        }

        return IsCancelled(
            errorString = "Cancellable.isCancelled() check is useless in a method annotated with ignoreCancelled=true.",
            fix = { expression.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText("false", expression)) }
        )
    }

    @Contract(value = "null -> false", pure = true)
    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        if (element.parent !is PsiClass) {
            return false
        }

        val project = element.project

        val psiClass = element.parent as PsiClass

        val javaPluginClass = JavaPsiFacade.getInstance(project)
            .findClass(BukkitConstants.JAVA_PLUGIN, GlobalSearchScope.allScope(project))

        return javaPluginClass != null && Arrays.stream(psiClass.extendsListTypes)
            .anyMatch { c -> c == PsiTypesUtil.getClassType(javaPluginClass) }
    }

    override fun dispose() {
        super.dispose()

        pluginYml = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as BukkitModule<*>?
        return Objects.equal(pluginYml, that!!.pluginYml) &&
            type === that.type &&
            Objects.equal(moduleType, that.moduleType)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(pluginYml, type, moduleType)
    }

    companion object {
        fun generateBukkitStyleEventListenerMethod(chosenClass: PsiClass,
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
                annotation.setDeclaredAttributeValue("ignoreCancelled", value)
            }

            return newMethod
        }
    }
}
