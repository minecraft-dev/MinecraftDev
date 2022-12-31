/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.stepSequence
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

fun FixedAssetsNewProjectWizardStep.addTemplates(project: Project, vararg templates: Pair<String, String>) {
    addTemplates(project, templates.toMap())
}

fun FixedAssetsNewProjectWizardStep.addTemplates(project: Project, templates: Map<String, String>) {
    val manager = FileTemplateManager.getInstance(project)
    addAssets(templates.map { (path, template) -> GeneratorTemplateFile(path, manager.getJ2eeTemplate(template)) })
}

fun splitPackage(text: String): Pair<String, String> {
    val index = text.lastIndexOf('.')
    val className = text.substring(index + 1)
    val packageName = text.substring(0, index)
    return packageName to className
}

private val stepClassToKey = mutableMapOf<Class<*>, Key<*>>()

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T: NewProjectWizardStep> getOrCreateClassKey(clazz: Class<T>) = stepClassToKey.computeIfAbsent(clazz) {
    Key.create<T>(it.name)
} as Key<T>

private val stepClassToWhenAvailableKey = mutableMapOf<Class<*>, Key<*>>()

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T: NewProjectWizardStep> getWhenAvailableKey(clazz: Class<T>) = stepClassToWhenAvailableKey[clazz] as Key<MutableList<(T) -> Unit>>?

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T: NewProjectWizardStep> getOrCreateWhenAvailableKey(clazz: Class<T>) = stepClassToWhenAvailableKey.computeIfAbsent(clazz) {
    Key.create<T>(it.name)
} as Key<MutableList<(T) -> Unit>>

inline fun <reified T: NewProjectWizardStep> T.storeToData() {
    data.putUserData(getOrCreateClassKey(T::class.java), this)
    getWhenAvailableKey(T::class.java)?.let { whenAvailableKey ->
        data.getUserData(whenAvailableKey)?.let { whenAvailable ->
            for (func in whenAvailable) {
                func(this)
            }
            data.putUserData(whenAvailableKey, null)
        }
    }
}

inline fun <reified T: NewProjectWizardStep> NewProjectWizardStep.findStep(): T {
    return data.getUserData(getOrCreateClassKey(T::class.java)) ?: throw IllegalStateException("Could not find required step ${T::class.java.name}")
}

inline fun <reified T: NewProjectWizardStep> NewProjectWizardStep.whenStepAvailable(noinline func: (T) -> Unit) {
    val value = data.getUserData(getOrCreateClassKey(T::class.java))
    if (value != null) {
        func(value)
    } else {
        val whenAvailableKey = getOrCreateWhenAvailableKey(T::class.java)
        val whenAvailable = data.getUserData(whenAvailableKey) ?: mutableListOf<(T) -> Unit>().also { data.putUserData(whenAvailableKey, it) }
        whenAvailable += func
    }
}

fun <T1, T2, T3, T4, T5, T6> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    return stepSequence(this, s1, s2, s3, s4, s5)
}

fun <T1, T2, T3, T4, T5, T6, T7> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    return stepSequence(this, s1, s2, s3, s4, s5, s6)
}

fun <T1, T2, T3, T4, T5, T6, T7, T8> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7, f7: (T7) -> T8): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep, T8: NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    val s7 = f7(s6)
    return stepSequence(this, s1, s2, s3, s4, s5, s6, s7)
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7, f7: (T7) -> T8, f8: (T8) -> T9): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep, T8: NewProjectWizardStep, T9: NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    val s7 = f7(s6)
    val s8 = f8(s7)
    return stepSequence(this, s1, s2, s3, s4, s5, s6, s7, s8)
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7, f7: (T7) -> T8, f8: (T8) -> T9, f9: (T9) -> T10): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep, T8: NewProjectWizardStep, T9: NewProjectWizardStep, T10: NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    val s7 = f7(s6)
    val s8 = f8(s7)
    val s9 = f9(s8)
    return stepSequence(this, s1, s2, s3, s4, s5, s6, s7, s8, s9)
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7, f7: (T7) -> T8, f8: (T8) -> T9, f9: (T9) -> T10, f10: (T10) -> T11): NewProjectWizardStep
    where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep, T8: NewProjectWizardStep, T9: NewProjectWizardStep, T10: NewProjectWizardStep, T11: NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    val s7 = f7(s6)
    val s8 = f8(s7)
    val s9 = f9(s8)
    val s10 = f10(s9)
    return stepSequence(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10)
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> T1.chain(f1: (T1) -> T2, f2: (T2) -> T3, f3: (T3) -> T4, f4: (T4) -> T5, f5: (T5) -> T6, f6: (T6) -> T7, f7: (T7) -> T8, f8: (T8) -> T9, f9: (T9) -> T10, f10: (T10) -> T11, f11: (T11) -> T12): NewProjectWizardStep
        where T1 : NewProjectWizardStep, T2 : NewProjectWizardStep, T3 : NewProjectWizardStep, T4 : NewProjectWizardStep, T5 : NewProjectWizardStep, T6 : NewProjectWizardStep, T7 : NewProjectWizardStep, T8: NewProjectWizardStep, T9: NewProjectWizardStep, T10: NewProjectWizardStep, T11: NewProjectWizardStep, T12: NewProjectWizardStep {

    val s1 = f1(this)
    val s2 = f2(s1)
    val s3 = f3(s2)
    val s4 = f4(s3)
    val s5 = f5(s4)
    val s6 = f6(s5)
    val s7 = f7(s6)
    val s8 = f8(s7)
    val s9 = f9(s8)
    val s10 = f10(s9)
    val s11 = f11(s10)
    return stepSequence(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11)
}
