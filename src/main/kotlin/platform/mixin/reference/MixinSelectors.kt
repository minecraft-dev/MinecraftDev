/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.ClassAndFieldNode
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findField
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import java.util.regex.PatternSyntaxException
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

fun parseMixinSelector(value: String): MixinSelector? {
    for (parser in MixinSelectorParser.EP_NAME.extensionList) {
        parser.parse(value)?.let { return it }
    }
    return null
}

interface MixinSelectorParser {
    fun parse(value: String): MixinSelector?

    companion object {
        val EP_NAME = ExtensionPointName.create<MixinSelectorParser>("com.demonwav.minecraft-dev.mixinSelectorParser")
    }
}

interface MixinSelector {
    fun matchField(owner: String, name: String, desc: String): Boolean
    fun matchMethod(owner: String, name: String, desc: String): Boolean

    fun matchField(field: PsiField, qualifier: PsiClass): Boolean {
        val fqn = qualifier.fullQualifiedName ?: return false
        val desc = field.descriptor ?: return false
        return matchField(fqn.replace('.', '/'), field.name, desc)
    }
    fun matchField(field: FieldNode, qualifier: ClassNode): Boolean {
        return matchField(qualifier.name, field.name, field.desc)
    }

    fun matchMethod(method: PsiMethod, qualifier: PsiClass): Boolean {
        val fqn = qualifier.fullQualifiedName ?: return false
        val desc = method.descriptor ?: return false
        return matchMethod(fqn.replace('.', '/'), method.internalName, desc)
    }
    fun matchMethod(method: MethodNode, qualifier: ClassNode): Boolean {
        return matchMethod(qualifier.name, method.name, method.desc)
    }

    /**
     * Implement this to return false for early-out optimizations, so you don't need to resolve the member in the
     * navigation visitor
     */
    fun canEverMatch(name: String): Boolean {
        return true
    }

    val owner: String?
    val descriptor: String?
    val qualified
        get() = owner != null

    val displayName: String

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
        val owner = this.owner
            ?: throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")

        fun doFind(owner: String): MixinTargetMember? {
            if (owner == CommonClassNames.JAVA_LANG_OBJECT) {
                return null
            }
            return RecursionManager.doPreventingRecursion(owner, false) {
                val classNode = findQualifiedClass(project, owner, scope)?.bytecode ?: return@doPreventingRecursion null

                classNode.findMethod(this)?.let {
                    return@doPreventingRecursion MethodTargetMember(null, ClassAndMethodNode(classNode, it))
                }

                classNode.findField(this)?.let {
                    return@doPreventingRecursion FieldTargetMember(null, ClassAndFieldNode(classNode, it))
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

        return doFind(owner)
    }

    private inline fun <R> resolve(project: Project, scope: GlobalSearchScope, ret: (PsiClass, PsiMember) -> R): R? {
        val owner = this.owner
            ?: throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")

        val psiClass = findQualifiedClass(project, owner, scope) ?: return null

        val field = psiClass.findField(this, checkBases = true)
        return if (field != null) {
            ret(psiClass, field)
        } else {
            psiClass.findMethods(this, checkBases = true).firstOrNull()?.let { ret(psiClass, it) }
        }
    }
}

// Member reference

fun MemberReference.toMixinString(): String {
    return buildString {
        if (owner != null) {
            append('L').append(owner.replace('.', '/')).append(';')
        }

        append(if (matchAllNames) "*" else name)

        descriptor?.let { descriptor ->
            if (!descriptor.startsWith('(')) {
                // Field descriptor
                append(':')
            }

            append(descriptor)
        }
    }
}

class MixinMemberParser : MixinSelectorParser {
    override fun parse(value: String): MixinSelector? {
        val reference = value.replace(" ", "")
        val owner: String?

        var pos = reference.lastIndexOf('.')
        if (pos != -1) {
            // Everything before the dot is the qualifier/owner
            owner = reference.substring(0, pos).replace('/', '.')
        } else {
            pos = reference.indexOf(';')
            if (pos != -1 && reference.startsWith('L')) {
                val internalOwner = reference.substring(1, pos)
                if (internalOwner.contains('.')) {
                    // Invalid: Qualifier should only contain slashes
                    return null
                }

                owner = internalOwner.replace('/', '.')
            } else {
                // No owner/qualifier specified
                pos = -1
                owner = null
            }
        }

        val descriptor: String?
        val name: String
        val matchAllNames = reference.getOrNull(pos + 1) == '*'
        val matchAllDescs: Boolean

        // Find descriptor separator
        val methodDescPos = reference.indexOf('(', pos + 1)
        if (methodDescPos != -1) {
            // Method descriptor
            descriptor = reference.substring(methodDescPos)
            name = reference.substring(pos + 1, methodDescPos)
            matchAllDescs = false
        } else {
            val fieldDescPos = reference.indexOf(':', pos + 1)
            if (fieldDescPos != -1) {
                descriptor = reference.substring(fieldDescPos + 1)
                name = reference.substring(pos + 1, fieldDescPos)
                matchAllDescs = false
            } else {
                descriptor = null
                matchAllDescs = reference.endsWith('*')
                name = if (matchAllDescs) {
                    reference.substring(pos + 1, reference.lastIndex)
                } else {
                    reference.substring(pos + 1)
                }
            }
        }

        return MemberReference(if (matchAllNames) "*" else name, descriptor, owner, matchAllNames, matchAllDescs)
    }
}

// Regex reference

class MixinRegexParser : MixinSelectorParser {
    override fun parse(value: String): MixinSelector? {
        if (!value.endsWith("/")) {
            return null
        }
        var foundAny = false
        var ownerPattern = MATCH_EVERYTHING
        var namePattern = MATCH_EVERYTHING
        var descPattern = MATCH_EVERYTHING
        for (match in PATTERN.findAll(value)) {
            foundAny = true
            val pattern = match.groups[3]!!.value
            when (match.groups[2]?.value) {
                "owner" -> ownerPattern = pattern.safeToRegex()
                "name" -> namePattern = pattern.safeToRegex()
                "desc" -> descPattern = pattern.safeToRegex()
                null -> namePattern = pattern.safeToRegex()
                else -> throw AssertionError() // should be covered by the pattern
            }
        }

        if (!foundAny) {
            return null
        }

        return MixinRegexSelector(
            ownerPattern,
            namePattern,
            descPattern,
            ownerPattern.getConstantString(),
            descPattern.getConstantString()
        )
    }

    private fun String.safeToRegex(): Regex {
        return try {
            toRegex()
        } catch (e: PatternSyntaxException) {
            MATCH_EVERYTHING
        }
    }

    private fun Regex.getConstantString(): String? {
        val pattern = this.pattern
        if (!pattern.startsWith("^") || !pattern.endsWith("$")) {
            return null
        }
        var entirePattern = pattern.substring(1, pattern.length - 1)
        if (SPECIAL_CHARS.containsMatchIn(entirePattern)) {
            return null
        }
        entirePattern = entirePattern.replace(UNESCAPED_BACKSLASH, "")
        entirePattern = entirePattern.replace("\\\\", "\\")
        return entirePattern
    }

    companion object {
        private val MATCH_EVERYTHING = ".*".toRegex()
        private val PATTERN = "((owner|name|desc)\\s*=\\s*)?/(.*?)(?<!\\\\)/".toRegex()
        private val SPECIAL_CHARS = "(?<!\\\\)(?:\\\\\\\\)*[\\^\$.|?*+()\\[\\]{}]".toRegex()
        private val UNESCAPED_BACKSLASH = "(?<!\\\\)\\\\(?!(\\\\\\\\)*\\\\)".toRegex()
    }
}

data class MixinRegexSelector(
    val ownerPattern: Regex,
    val namePattern: Regex,
    val descPattern: Regex,
    override val owner: String?,
    override val descriptor: String?
) : MixinSelector {
    override fun matchField(owner: String, name: String, desc: String): Boolean {
        return ownerPattern.containsMatchIn(owner) &&
            namePattern.containsMatchIn(name) &&
            descPattern.containsMatchIn(desc)
    }

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        return ownerPattern.containsMatchIn(owner) &&
            namePattern.containsMatchIn(name) &&
            descPattern.containsMatchIn(desc)
    }

    override fun canEverMatch(name: String): Boolean {
        return namePattern.containsMatchIn(name)
    }

    override val displayName: String
        get() = namePattern.pattern
}
