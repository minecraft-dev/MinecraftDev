/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.platform.mixin.util.ClassAndFieldNode
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import java.io.Serializable
import java.lang.reflect.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * Represents a reference to a class member (a method or a field). It may
 * resolve to multiple members if [matchAllNames] or [matchAllDescs] is set or if the member is
 * not full qualified.
 */
data class MemberReference(
    val name: String,
    val descriptor: String? = null,
    val owner: String? = null,
    val matchAllNames: Boolean = false,
    val matchAllDescs: Boolean = false
) : Serializable {

    val qualified
        get() = this.owner != null

    val withoutOwner
        get() = if (this.owner == null) {
            this
        } else {
            MemberReference(this.name, this.descriptor, null, this.matchAllNames, this.matchAllDescs)
        }

    fun matchOwner(psiClass: PsiClass): Boolean {
        return this.owner == null || this.owner == psiClass.fullQualifiedName
    }

    fun matchOwner(clazz: ClassNode): Boolean {
        return this.owner == null || this.owner == clazz.name.replace('/', '.')
    }

    fun match(method: PsiMethod, qualifier: PsiClass): Boolean {
        return (this.matchAllNames || this.name == method.internalName) && matchOwner(qualifier) &&
            (this.descriptor == null || this.descriptor == method.descriptor)
    }

    fun match(method: MethodNode, qualifier: ClassNode): Boolean {
        return (this.matchAllNames || this.name == method.name) && matchOwner(qualifier) &&
            (this.descriptor == null || this.descriptor == method.desc)
    }

    fun match(field: PsiField, qualifier: PsiClass): Boolean {
        return (this.matchAllNames || this.name == field.name) && matchOwner(qualifier) &&
            (this.descriptor == null || this.descriptor == field.descriptor)
    }

    fun match(field: FieldNode, qualifier: ClassNode): Boolean {
        return (this.matchAllNames || this.name == field.name) && matchOwner(qualifier) &&
            (this.descriptor == null || this.descriptor == field.desc)
    }

    fun resolve(
        project: Project,
        scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
    ): Pair<PsiClass, PsiMember>? {
        return resolve(project, scope, ::Pair)
    }

    fun resolveMember(project: Project, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)): PsiMember? {
        return resolve(project, scope) { _, member -> member }
    }

    fun resolveAsm(
        project: Project,
        scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
    ): MixinTargetMember? {
        if (this.owner == null) {
            throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")
        }

        fun doFind(owner: String): MixinTargetMember? {
            if (owner == CommonClassNames.JAVA_LANG_OBJECT) {
                return null
            }
            return RecursionManager.doPreventingRecursion(owner, false) {
                val classNode = findQualifiedClass(project, owner, scope)?.bytecode ?: return@doPreventingRecursion null

                if (descriptor == null || descriptor.startsWith("(")) {
                    classNode.findMethod(this)?.let {
                        return@doPreventingRecursion MethodTargetMember(null, ClassAndMethodNode(classNode, it))
                    }
                }

                if (descriptor == null || !descriptor.startsWith("(")) {
                    classNode.fields?.firstOrNull {
                        (matchAllNames || name == it.name) && (descriptor == null || it.desc == descriptor)
                    }?.let {
                        return@doPreventingRecursion FieldTargetMember(null, ClassAndFieldNode(classNode, it))
                    }
                }

                classNode.superName?.let { doFind(it.replace('/', '.')) }?.let { return@doPreventingRecursion it }

                classNode.interfaces?.let { interfaces ->
                    for (itf in interfaces) {
                        doFind(itf.replace('/', '.'))?.let { return@doPreventingRecursion it }
                    }
                }

                null
            }
        }

        return doFind(this.owner)
    }

    private inline fun <R> resolve(project: Project, scope: GlobalSearchScope, ret: (PsiClass, PsiMember) -> R): R? {
        if (this.owner == null) {
            throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")
        }

        val psiClass = findQualifiedClass(project, this.owner, scope) ?: return null

        val member: PsiMember? = if (descriptor != null && descriptor.startsWith('(')) {
            // Method, we assume there is only one (since this member descriptor is full qualified)
            psiClass.findMethods(this, checkBases = true).firstOrNull()
        } else {
            // Field
            psiClass.findField(this, checkBases = true)
        }

        return member?.let { ret(psiClass, member) }
    }

    object Deserializer : JsonDeserializer<MemberReference> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): MemberReference {
            val ref = json.asString
            val className = ref.substringBefore('#')
            val methodName = ref.substring(className.length + 1, ref.indexOf("("))
            val methodDesc = ref.substring(className.length + methodName.length + 1)
            return MemberReference(methodName, methodDesc, className)
        }
    }
}

// Class

fun PsiClass.findMethods(member: MemberReference, checkBases: Boolean = false): Sequence<PsiMethod> {
    if (!member.matchOwner(this)) {
        return emptySequence()
    }

    val result = if (member.matchAllNames) {
        if (checkBases) {
            allMethods + constructors
        } else {
            methods + constructors
        }
    } else {
        findMethodsByInternalName(member.name, checkBases)
    }
    return if (member.descriptor != null) {
        result.asSequence().filter { it.descriptor == member.descriptor }
    } else {
        result.asSequence()
    }
}

fun PsiClass.findField(member: MemberReference, checkBases: Boolean = false): PsiField? {
    if (!member.matchOwner(this)) {
        return null
    }

    val fields = if (member.matchAllNames) {
        if (checkBases) {
            allFields.toList()
        } else {
            fields.toList()
        }
    } else {
        listOfNotNull(findFieldByName(member.name, checkBases))
    }
    if (member.descriptor == null) {
        return fields.firstOrNull()
    }
    return fields.firstOrNull { it.descriptor == member.descriptor }
}

// Method

val PsiMethod.memberReference
    get() = MemberReference(internalName, descriptor)

val PsiMethod.qualifiedMemberReference
    get() = MemberReference(internalName, descriptor, containingClass?.fullQualifiedName)

fun PsiMethod.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return getQualifiedMemberReference(owner.fullQualifiedName)
}

fun PsiMethod.getQualifiedMemberReference(owner: String?): MemberReference {
    return MemberReference(internalName, descriptor, owner)
}

fun PsiMethod?.isSameReference(reference: PsiMethod?): Boolean =
    this != null && (this === reference || qualifiedMemberReference == reference?.qualifiedMemberReference)

// Field
val PsiField.simpleMemberReference
    get() = MemberReference(name)

val PsiField.memberReference
    get() = MemberReference(name, descriptor)

val PsiField.simpleQualifiedMemberReference
    get() = MemberReference(name, null, containingClass!!.fullQualifiedName)

val PsiField.qualifiedMemberReference
    get() = MemberReference(name, descriptor, containingClass!!.fullQualifiedName)

fun PsiField.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return getQualifiedMemberReference(owner.fullQualifiedName)
}

fun PsiField.getQualifiedMemberReference(owner: String?): MemberReference {
    return MemberReference(name, descriptor, owner)
}
