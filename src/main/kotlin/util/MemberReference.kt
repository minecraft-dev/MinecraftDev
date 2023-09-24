/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.io.Serializable
import java.lang.reflect.Type

/**
 * Represents a reference to a class member (a method or a field). It may
 * resolve to multiple members if [matchAllNames] or [matchAllDescs] is set or if the member is
 * not full qualified.
 */
data class MemberReference(
    val name: String,
    val descriptor: String? = null,
    override val owner: String? = null,
    val matchAllNames: Boolean = false,
    val matchAllDescs: Boolean = false,
) : Serializable, MixinSelector {

    init {
        assert(owner?.contains('/') != true)
    }

    val withoutDescriptor
        get() = if (this.descriptor == null) {
            this
        } else {
            copy(descriptor = null)
        }

    val withoutOwner
        get() = if (this.owner == null) {
            this
        } else {
            copy(owner = null)
        }

    override val methodDescriptor = descriptor?.takeIf { it.contains("(") }
    override val fieldDescriptor = descriptor?.takeUnless { it.contains("(") }
    override val displayName = name

    override fun canEverMatch(name: String): Boolean {
        return matchAllNames || this.name == name
    }

    private fun matchOwner(clazz: String): Boolean {
        assert(!clazz.contains('.'))
        return this.owner == null || this.owner == clazz.replace('/', '.')
    }

    override fun matchField(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return (this.matchAllNames || this.name == name) &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
    }

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return (this.matchAllNames || this.name == name) &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
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

fun PsiClass.findMethods(member: MixinSelector, checkBases: Boolean = false): Sequence<PsiMethod> {
    val methods = if (checkBases) {
        allMethods.asSequence()
    } else {
        methods.asSequence()
    } + constructors
    return methods.filter { member.matchMethod(it, this) }
}

fun PsiClass.findField(selector: MixinSelector, checkBases: Boolean = false): PsiField? {
    val fields = if (checkBases) {
        allFields.toList()
    } else {
        fields.toList()
    }
    return fields.firstOrNull { selector.matchField(it, this) }
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
