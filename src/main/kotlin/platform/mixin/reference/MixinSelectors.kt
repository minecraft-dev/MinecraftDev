/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DESC
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findField
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.resolveClass
import com.demonwav.mcdev.util.resolveType
import com.demonwav.mcdev.util.resolveTypeArray
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import java.util.Locale
import java.util.regex.PatternSyntaxException
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

fun parseMixinSelector(element: PsiElement): MixinSelector? {
    val stringValue = element.constantStringValue ?: return null
    return parseMixinSelector(stringValue, element)
}

fun parseMixinSelector(value: String, context: PsiElement): MixinSelector? {
    for (parser in MixinSelectorParser.EP_NAME.extensionList) {
        parser.parse(value, context)?.let { return it }
    }
    return null
}

/**
 * A parser which creates a selector from a string literal. Can be added via an extension point.
 * For custom dynamic selectors, you likely want to extend [DynamicSelectorParser].
 */
interface MixinSelectorParser {
    fun parse(value: String, context: PsiElement): MixinSelector?

    companion object {
        val EP_NAME = ExtensionPointName.create<MixinSelectorParser>("com.demonwav.minecraft-dev.mixinSelectorParser")
    }
}

/**
 * An interface which matches members, that's it really.
 */
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
    val methodDescriptor: String?
    val fieldDescriptor: String?
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
        val owner = this.owner ?: return null

        fun doFind(owner: String): MixinTargetMember? {
            if (owner == CommonClassNames.JAVA_LANG_OBJECT) {
                return null
            }
            return RecursionManager.doPreventingRecursion(owner, false) {
                val classNode = findQualifiedClass(project, owner, scope)?.bytecode ?: return@doPreventingRecursion null

                classNode.findMethod(this)?.let {
                    return@doPreventingRecursion MethodTargetMember(classNode, it)
                }

                classNode.findField(this)?.let {
                    return@doPreventingRecursion FieldTargetMember(classNode, it)
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
        val owner = this.owner ?: return null

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
    override fun parse(value: String, context: PsiElement): MixinSelector? {
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
                if (!StringUtil.isJavaIdentifier(internalOwner.replace('/', '_'))) {
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

        if (!matchAllNames && !StringUtil.isJavaIdentifier(name) && name != "<init>" && name != "<clinit>") {
            return null
        }

        return MemberReference(if (matchAllNames) "*" else name, descriptor, owner, matchAllNames, matchAllDescs)
    }
}

// Regex reference

class MixinRegexParser : MixinSelectorParser {
    override fun parse(value: String, context: PsiElement): MixinSelector? {
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

private class MixinRegexSelector(
    val ownerPattern: Regex,
    val namePattern: Regex,
    val descPattern: Regex,
    override val owner: String?,
    descriptor: String?
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

    override val methodDescriptor = descriptor?.takeIf { it.contains("(") }
    override val fieldDescriptor = descriptor?.takeUnless { it.contains("(") }

    override val displayName: String
        get() = namePattern.pattern
}

// Dynamic selectors

/**
 * Checks if the string uses a dynamic selector that exists in the project but has no special handling
 * in mcdev, used to suppress invalid selector errors.
 */
fun isMiscDynamicSelector(project: Project, value: String): Boolean {
    // check for dynamic selectors that aren't registered in extension points
    val matchResult = DYNAMIC_SELECTOR_PATTERN.find(value) ?: return false
    val id = matchResult.groups[1]!!.value
    for (parser in MixinSelectorParser.EP_NAME.extensionList) {
        if (parser is DynamicSelectorParser && parser.validIds.contains(id)) {
            return false
        }
    }
    return getAllDynamicSelectors(project).contains(id)
}

private fun getAllDynamicSelectors(project: Project): Set<String> {
    val selectorId = JavaPsiFacade.getInstance(project)
        .findClass(MixinConstants.Classes.SELECTOR_ID, GlobalSearchScope.allScope(project)) ?: return emptySet()
    return selectorId.cached(PsiModificationTracker.MODIFICATION_COUNT) {
        AnnotatedMembersSearch.search(selectorId).asSequence().flatMap { member ->
            if (member !is PsiClass) {
                return@flatMap emptySequence()
            }
            if (!InheritanceUtil.isInheritor(member, MixinConstants.Classes.TARGET_SELECTOR_DYNAMIC)) {
                return@flatMap emptySequence()
            }
            val annotation = member.findAnnotation(MixinConstants.Classes.SELECTOR_ID) ?: return@flatMap emptySequence()
            val value = annotation.findAttributeValue("value")?.constantStringValue
                ?: return@flatMap emptySequence()
            val namespace = annotation.findAttributeValue("namespace")?.constantStringValue
            if (namespace.isNullOrEmpty()) {
                val builtinPrefix = "org.spongepowered.asm.mixin.injection.selectors."
                if (member.qualifiedName?.startsWith(builtinPrefix) == true) {
                    sequenceOf(value, "mixin:$value")
                } else {
                    sequenceOf(value)
                }
            } else {
                sequenceOf("$namespace:$value")
            }
        }.toSet()
    }
}

private val DYNAMIC_SELECTOR_PATTERN = "(?i)^@([a-z]+(:[a-z]+)?)(\\((.*)\\))?$".toRegex()

abstract class DynamicSelectorParser(id: String, vararg aliases: String) : MixinSelectorParser {
    val validIds = aliases.toSet() + id

    final override fun parse(value: String, context: PsiElement): MixinSelector? {
        val matchResult = DYNAMIC_SELECTOR_PATTERN.find(value) ?: return null
        val id = matchResult.groups[1]!!.value
        if (!validIds.contains(id)) {
            return null
        }
        return parseDynamic(matchResult.groups[4]?.value ?: "", context)
    }

    abstract fun parseDynamic(args: String, context: PsiElement): MixinSelector?
}

// @Desc

class DescSelectorParser : DynamicSelectorParser("Desc", "mixin:Desc") {
    override fun parseDynamic(args: String, context: PsiElement): MixinSelector? {
        val descAnnotation = findDescAnnotation(args.lowercase(Locale.ENGLISH), context) ?: return null
        return descSelectorFromAnnotation(descAnnotation)
    }

    private fun findDescAnnotation(id: String, context: PsiElement): PsiAnnotation? {
        if (id.isNotEmpty() && id != "?") {
            // explicit id
            forEachDescAnnotationOwner(context) { annotationOwner ->
                findDescAnnotations(annotationOwner) { desc ->
                    val descId = desc.findAttributeValue("id")?.constantStringValue?.lowercase(Locale.ENGLISH)
                    if (descId == id) {
                        return desc
                    }
                }
            }
            return null
        } else {
            // implicit coordinates
            val childOwners = mutableListOf<PsiElement>()
            var coordinate = ""
            forEachDescAnnotationOwner(context) { annotationOwner ->
                childOwners.add(annotationOwner)
                if (coordinate.isNotEmpty()) {
                    for (owner in childOwners) {
                        findDescAnnotations(owner) { desc ->
                            val descId = desc.findAttributeValue("id")?.constantStringValue?.lowercase(Locale.ENGLISH)
                            if (descId == coordinate) {
                                return desc
                            }
                        }
                    }
                }
                val nextCoordinate = getCoordinate(annotationOwner)?.lowercase(Locale.ENGLISH)
                if (nextCoordinate != null) {
                    coordinate = if (coordinate.isEmpty()) {
                        nextCoordinate
                    } else {
                        "$nextCoordinate.$coordinate"
                    }
                }
            }

            return null
        }
    }

    private fun getCoordinate(element: PsiElement): String? {
        return when (element) {
            is PsiAnnotation -> {
                val name = element.parentOfType<PsiNameValuePair>()?.name ?: return null
                if (element.hasQualifiedName(SLICE)) {
                    val sliceId = element.findAttributeValue("id")?.constantStringValue
                    if (!sliceId.isNullOrEmpty()) {
                        "$name.$sliceId"
                    } else {
                        name
                    }
                } else {
                    name
                }
            }
            is PsiMethod -> {
                element.name
            }
            else -> null
        }
    }

    private inline fun forEachDescAnnotationOwner(context: PsiElement, handler: (PsiElement) -> Unit) {
        var element: PsiElement? = context.parentOfType<PsiAnnotation>()
        while (element != null) {
            handler(element)
            if (element is PsiClass) {
                break
            }
            element = PsiTreeUtil.getParentOfType(
                element,
                PsiAnnotation::class.java,
                PsiMethod::class.java,
                PsiClass::class.java
            )
        }
    }

    private inline fun findDescAnnotations(element: PsiElement, handler: (PsiAnnotation) -> Unit) {
        when (element) {
            is PsiAnnotation -> {
                val desc = element.findAttributeValue("desc") as? PsiAnnotation ?: return
                if (!desc.hasQualifiedName(DESC)) return
                handler(desc)
            }
            is PsiMethod -> {
                for (annotation in element.modifierList.applicableAnnotations) {
                    if (annotation.hasQualifiedName(DESC)) {
                        handler(annotation)
                    }
                }
            }
            is PsiClass -> {
                val modifierList = element.modifierList ?: return
                for (annotation in modifierList.applicableAnnotations) {
                    if (annotation.hasQualifiedName(DESC)) {
                        handler(annotation)
                    }
                }
            }
        }
    }

    companion object {
        fun descSelectorFromAnnotation(descAnnotation: PsiAnnotation): DescSelector? {
            val explicitOwner = descAnnotation.findAttributeValue("owner")
                ?.resolveClass()?.fullQualifiedName?.replace('.', '/')
            val owners = if (explicitOwner != null) {
                setOf(explicitOwner)
            } else {
                descAnnotation.findContainingClass()?.mixinTargets?.mapTo(mutableSetOf()) { it.name } ?: return null
            }
            if (owners.isEmpty()) {
                return null
            }

            val name = descAnnotation.findAttributeValue("value")?.constantStringValue ?: return null

            val argTypes = descAnnotation.findAttributeValue("args")?.resolveTypeArray() ?: emptyList()
            val ret = descAnnotation.findAttributeValue("ret")?.resolveType() ?: PsiType.VOID
            val desc = Type.getMethodDescriptor(
                Type.getType(ret.descriptor),
                *argTypes.mapToArray { Type.getType(it.descriptor) }
            )

            return DescSelector(owners, name, desc)
        }
    }
}

data class DescSelector(
    val owners: Set<String>,
    val name: String,
    override val methodDescriptor: String
) : MixinSelector {
    override fun matchField(owner: String, name: String, desc: String): Boolean {
        return this.owners.contains(owner) && this.name == name && this.fieldDescriptor.substringBefore("(") == desc
    }

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        return this.owners.contains(owner) && this.name == name && this.methodDescriptor == desc
    }

    override fun canEverMatch(name: String): Boolean {
        return this.name == name
    }

    override val owner = owners.singleOrNull()
    override val fieldDescriptor = methodDescriptor.substringBefore('(')
    override val displayName = name
}
