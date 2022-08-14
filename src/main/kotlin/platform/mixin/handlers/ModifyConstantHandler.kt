/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.ConstantInjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CONSTANT_CONDITION
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.parseArray
import com.demonwav.mcdev.util.resolveClass
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyConstantHandler : InjectorAnnotationHandler() {
    private val allowedOpcodes = setOf(
        Opcodes.ICONST_M1,
        Opcodes.ICONST_0,
        Opcodes.ICONST_1,
        Opcodes.ICONST_2,
        Opcodes.ICONST_3,
        Opcodes.ICONST_4,
        Opcodes.ICONST_5,
        Opcodes.LCONST_0,
        Opcodes.LCONST_1,
        Opcodes.FCONST_0,
        Opcodes.FCONST_1,
        Opcodes.FCONST_2,
        Opcodes.DCONST_0,
        Opcodes.DCONST_1,
        Opcodes.BIPUSH,
        Opcodes.SIPUSH,
        Opcodes.LDC,
        Opcodes.IFLT,
        Opcodes.IFGE,
        Opcodes.IFGT,
        Opcodes.IFLE
    )

    private class ModifyConstantInfo(
        val constantInfo: ConstantInjectionPoint.ConstantInfo,
        val constantAnnotation: PsiAnnotation
    )

    private fun getConstantInfos(modifyConstant: PsiAnnotation): List<ModifyConstantInfo>? {
        val constants = modifyConstant.findDeclaredAttributeValue("constant")
            ?.findAnnotations()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return constants.map { constant ->
            val nullValue = constant.findDeclaredAttributeValue("nullValue")?.constantValue as? Boolean ?: false
            val intValue = (constant.findDeclaredAttributeValue("intValue")?.constantValue as? Number)?.toInt()
            val floatValue = (constant.findDeclaredAttributeValue("floatValue")?.constantValue as? Number)?.toFloat()
            val longValue = (constant.findDeclaredAttributeValue("longValue")?.constantValue as? Number)?.toLong()
            val doubleValue = (constant.findDeclaredAttributeValue("doubleValue")?.constantValue as? Number)?.toDouble()
            val stringValue = constant.findDeclaredAttributeValue("stringValue")?.constantValue as? String
            val classValue = constant.findDeclaredAttributeValue("classValue")?.resolveClass()?.descriptor?.let {
                Type.getType(
                    it
                )
            }

            fun Boolean.toInt(): Int {
                return if (this) 1 else 0
            }

            val count = nullValue.toInt() +
                (intValue != null).toInt() +
                (floatValue != null).toInt() +
                (longValue != null).toInt() +
                (doubleValue != null).toInt() +
                (stringValue != null).toInt() +
                (classValue != null).toInt()
            if (count != 1) {
                return null
            }

            val value = if (nullValue) {
                null
            } else {
                intValue ?: floatValue ?: longValue ?: doubleValue ?: stringValue ?: classValue
            }

            val expandConditions = constant.findDeclaredAttributeValue("expandZeroConditions")?.parseArray {
                if (it !is PsiReferenceExpression) {
                    return@parseArray null
                }
                val field = it.resolve() as? PsiEnumConstant ?: return@parseArray null
                val enumClass = field.containingClass ?: return@parseArray null
                if (enumClass.fullQualifiedName != CONSTANT_CONDITION) {
                    return@parseArray null
                }
                try {
                    ConstantInjectionPoint.ExpandCondition.valueOf(field.name)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }?.toSet() ?: emptySet()

            ModifyConstantInfo(ConstantInjectionPoint.ConstantInfo(value, expandConditions), constant)
        }
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>? {
        val constantInfos = getConstantInfos(annotation) ?: return null
        val psiManager = PsiManager.getInstance(annotation.project)
        return constantInfos.asSequence().map {
            when (it.constantInfo.constant) {
                null -> PsiType.getJavaLangObject(psiManager, annotation.resolveScope)
                is Int -> PsiType.INT
                is Float -> PsiType.FLOAT
                is Long -> PsiType.LONG
                is Double -> PsiType.DOUBLE
                is String -> PsiType.getJavaLangString(psiManager, annotation.resolveScope)
                is Type -> PsiType.getJavaLangClass(psiManager, annotation.resolveScope)
                else -> throw IllegalStateException("Unknown constant type: ${it.constantInfo.constant.javaClass.name}")
            }
        }.distinct().map { type ->
            MethodSignature(
                listOf(
                    ParameterGroup(listOf(sanitizedParameter(type, "constant"))),
                    ParameterGroup(
                        collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                        isVarargs = true,
                        required = ParameterGroup.RequiredLevel.OPTIONAL
                    )
                ),
                type
            )
        }.toList()
    }

    override fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<PsiElement> {
        val constantInfos = getConstantInfos(annotation) ?: return emptyList()

        val targetElement = targetMethod.findSourceElement(
            targetClass,
            annotation.project,
            GlobalSearchScope.allScope(annotation.project),
            canDecompile = true
        ) ?: return emptyList()

        val constantInjectionPoint = InjectionPoint.byAtCode("CONSTANT") as? ConstantInjectionPoint
            ?: return emptyList()

        return constantInfos.asSequence().flatMap { modifyConstantInfo ->
            val collectVisitor = ConstantInjectionPoint.MyCollectVisitor(
                annotation.project,
                CollectVisitor.Mode.MATCH_ALL,
                modifyConstantInfo.constantInfo
            )
            constantInjectionPoint.addStandardFilters(
                modifyConstantInfo.constantAnnotation,
                targetClass,
                collectVisitor
            )
            collectVisitor.visit(targetMethod)
            val bytecodeResults = collectVisitor.result

            val navigationVisitor = ConstantInjectionPoint.MyNavigationVisitor(modifyConstantInfo.constantInfo)
            targetElement.accept(navigationVisitor)

            bytecodeResults.asSequence().mapNotNull { bytecodeResult ->
                navigationVisitor.result.getOrNull(bytecodeResult.index)
            }
        }.sortedBy { it.textOffset }.toList()
    }

    override fun resolveInstructions(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        mode: CollectVisitor.Mode
    ): List<CollectVisitor.Result<*>> {
        val constantInfos = getConstantInfos(annotation) ?: return emptyList()
        val constantInjectionPoint = InjectionPoint.byAtCode("CONSTANT") as? ConstantInjectionPoint
            ?: return emptyList()
        return constantInfos.asSequence().flatMap { modifyConstantInfo ->
            val collectVisitor = ConstantInjectionPoint.MyCollectVisitor(
                annotation.project,
                mode,
                modifyConstantInfo.constantInfo
            )
            constantInjectionPoint.addStandardFilters(
                modifyConstantInfo.constantAnnotation,
                targetClass,
                collectVisitor
            )
            collectVisitor.visit(targetMethod)
            collectVisitor.result.asSequence()
        }.sortedBy { targetMethod.instructions.indexOf(it.insn) }.toList()
    }

    override fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): InsnResolutionInfo.Failure? {
        val constantInfos = getConstantInfos(annotation) ?: return InsnResolutionInfo.Failure()
        val constantInjectionPoint = InjectionPoint.byAtCode("CONSTANT") as? ConstantInjectionPoint
            ?: return null
        return constantInfos.asSequence().mapNotNull { modifyConstantInfo ->
            val collectVisitor = ConstantInjectionPoint.MyCollectVisitor(
                annotation.project,
                CollectVisitor.Mode.MATCH_FIRST,
                modifyConstantInfo.constantInfo
            )
            constantInjectionPoint.addStandardFilters(
                modifyConstantInfo.constantAnnotation,
                targetClass,
                collectVisitor
            )
            collectVisitor.visit(targetMethod)
            if (collectVisitor.result.isEmpty()) {
                collectVisitor.filterToBlame
            } else {
                null
            }
        }.firstOrNull()?.let(InsnResolutionInfo::Failure)
    }

    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return insn.opcode in allowedOpcodes
    }
}
