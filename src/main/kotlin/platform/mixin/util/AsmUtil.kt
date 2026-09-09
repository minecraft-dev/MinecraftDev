/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.util.MemberMatcher
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Quantifier
import com.demonwav.mcdev.util.anonymousClasses
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.hasSyntheticMethod
import com.demonwav.mcdev.util.innerIndexAndName
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.demonwav.mcdev.util.localClasses
import com.demonwav.mcdev.util.lockedCached
import com.demonwav.mcdev.util.loggerForTopLevel
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.byteCodeViewer.ByteCodeViewerManager
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.java.syntax.parser.JavaKeywords
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiParameterListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.LambdaRefactoringUtil
import com.intellij.util.CommonJavaRefactoringUtil
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceAnnotationVisitor
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor

private val LOGGER = loggerForTopLevel()

private val MODIFIER_TO_ACCESS_FLAG = mapOf(
    entry(PsiModifier.PUBLIC, Opcodes.ACC_PUBLIC),
    entry(PsiModifier.PROTECTED, Opcodes.ACC_PROTECTED),
    entry(PsiModifier.PRIVATE, Opcodes.ACC_PRIVATE),
    entry(PsiModifier.STATIC, Opcodes.ACC_STATIC),
    entry(PsiModifier.ABSTRACT, Opcodes.ACC_ABSTRACT),
    entry(PsiModifier.FINAL, Opcodes.ACC_FINAL),
    entry(PsiModifier.NATIVE, Opcodes.ACC_NATIVE),
    entry(PsiModifier.SYNCHRONIZED, Opcodes.ACC_SYNCHRONIZED),
    entry(PsiModifier.STRICTFP, Opcodes.ACC_STRICT),
    entry(PsiModifier.TRANSIENT, Opcodes.ACC_TRANSIENT),
    entry(PsiModifier.VOLATILE, Opcodes.ACC_VOLATILE),
    entry(PsiModifier.OPEN, Opcodes.ACC_OPEN),
    entry(PsiModifier.TRANSITIVE, Opcodes.ACC_TRANSITIVE),
)

// Kotlin 1.6.0 understands TYPE_USE now so won't allow the @ModifierConstant annotation in the map definition anymore
private fun entry(@PsiModifier.ModifierConstant modifierConstant: String, access: Int): Pair<String, Int> {
    return modifierConstant to access
}

@PsiUtil.AccessLevel
private fun accessLevelFromFlags(access: Int): Int {
    return when {
        (access and Opcodes.ACC_PUBLIC) != 0 -> PsiUtil.ACCESS_LEVEL_PUBLIC
        (access and Opcodes.ACC_PROTECTED) != 0 -> PsiUtil.ACCESS_LEVEL_PROTECTED
        (access and Opcodes.ACC_PRIVATE) != 0 -> PsiUtil.ACCESS_LEVEL_PRIVATE
        else -> PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL
    }
}

private fun hasModifier(access: Int, @PsiModifier.ModifierConstant modifier: String): Boolean {
    val flag = MODIFIER_TO_ACCESS_FLAG[modifier] ?: return false
    return (access and flag) != 0
}

fun Type.toPsiType(elementFactory: PsiElementFactory, context: PsiElement? = null): PsiType {
    if (this == ExpressionASMUtils.INTLIKE_TYPE) {
        return PsiTypes.intType()
    }
    return elementFactory.createTypeFromText(canonicalName, context)
}

val Type.canonicalName get() = computeCanonicalName(this)

private val DOLLAR_TO_DOT_REGEX = "\\$(?!\\d)".toRegex()

private fun computeCanonicalName(type: Type): String {
    return when (type.sort) {
        Type.ARRAY -> computeCanonicalName(type.elementType) + "[]".repeat(type.dimensions)
        Type.OBJECT -> type.className.replace(DOLLAR_TO_DOT_REGEX, ".")
        else -> type.className
    }
}

val Type.isPrimitive get() = sort != Type.ARRAY && sort != Type.OBJECT && sort != Type.METHOD

private fun hasAccess(access: Int, flag: Int) = (access and flag) != 0

// ClassNode

fun ClassNode.hasAccess(flag: Int) = hasAccess(this.access, flag)

fun ClassNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

fun internalNameToShortName(internalName: String) = internalName.substringAfterLast('/').replace('$', '.')

val ClassNode.shortName
    get() = internalNameToShortName(name)

val Type.shortName get() = className.substringAfterLast('.').replace('$', '.')

fun shortDescString(desc: String) = Type.getArgumentTypes(desc).joinToString(prefix = "(", postfix = ")") {
    it.shortName
}

private val INNER_CLASS_NODES_KEY = Key.create<CachedValue<ConcurrentMap<String, ClassNode?>>>("mcdev.innerClassNodes")

/**
 * Tries to find the bytecode for the class for the given qualified name.
 */
fun findClassNodeByQualifiedName(project: Project, module: Module?, fqn: String): ClassNode? {
    val psiClass = findQualifiedClass(project, fqn)
    if (psiClass != null) {
        return findClassNodeByPsiClass(psiClass, module)
    }

    fun resolveViaFakeClass(): ClassNode? {
        val fakeClassNode = ClassNode()
        fakeClassNode.name = fqn.replace('.', '/')
        val fakePsiClass = fakeClassNode.constructClass(project, "") ?: return null
        return findClassNodeByPsiClass(fakePsiClass, module)
    }

    val outerClass = findQualifiedClass(project, fqn.substringBefore('$'))
    if (outerClass != null) {
        val innerClasses = outerClass.lockedCached(
            INNER_CLASS_NODES_KEY,
            compute = ::ConcurrentHashMap
        )
        return innerClasses.computeIfAbsent(fqn) { resolveViaFakeClass() }
    }

    return resolveViaFakeClass()
}

private val NODE_BY_PSI_CLASS_KEY = Key.create<CachedValue<ClassNode?>>("mcdev.nodeByPsiClass")

fun findClassNodeByPsiClass(psiClass: PsiClass, module: Module? = psiClass.findModule()): ClassNode? {
    return psiClass.lockedCached(NODE_BY_PSI_CLASS_KEY) {
        try {
            val bytes = ByteCodeViewerManager.findClassFile(psiClass)?.contentsToByteArray(false)
            if (bytes == null) {
                // find compiler output
                if (module == null) return@lockedCached null
                val fqn = psiClass.fullQualifiedName ?: return@lockedCached null
                var parentDir = CompilerModuleExtension.getInstance(module)?.compilerOutputPath
                    ?: return@lockedCached null
                val packageName = fqn.substringBeforeLast('.', "")
                if (packageName.isNotEmpty()) {
                    for (dir in packageName.split('.')) {
                        parentDir = parentDir.findChild(dir) ?: return@lockedCached null
                    }
                }
                val classFile = parentDir.findChild("${fqn.substringAfterLast('.')}.class")
                    ?: return@lockedCached null
                val node = ClassNode(Opcodes.ASM7)
                classFile.inputStream.use { ClassReader(it).accept(node, 0) }
                node
            } else {
                val node = ClassNode(Opcodes.ASM7)
                ClassReader(bytes).accept(node, 0)
                node
            }
        } catch (e: Throwable) {
            val actualThrowable = if (e is InvocationTargetException) e.cause ?: e else e
            if (actualThrowable is ProcessCanceledException) {
                throw actualThrowable
            }

            if (actualThrowable is NoSuchFileException) {
                return@lockedCached null
            }

            val message = actualThrowable.message
            // TODO: display an error to the user?
            if (message == null || !message.contains("Unsupported class file major version")) {
                LOGGER.error(actualThrowable)
            }
            null
        }
    }
}

private fun ClassNode.constructClass(project: Project, body: String): PsiClass? {
    val outerClassName = name.substringBefore('$')
    val packageName = outerClassName.substringBeforeLast('/', "").replace('/', '.')
    val outerClassSimpleName = outerClassName.substringAfterLast('/')
    val innerClasses = if (name.contains('$')) {
        name.substringAfter('$').split('$')
    } else {
        emptyList()
    }
    val text = buildString {
        if (packageName.isNotEmpty()) {
            append("package ")
            append(packageName)
            append(";\n\n")
        }

        append("public class ")
        append(outerClassSimpleName)
        append(" {\n")
        var indent = "   "
        val closingBraces = mutableListOf("}")
        for ((index, innerClass) in innerClasses.withIndex()) {
            val innerIndexAndName = innerIndexAndName(innerClass)
            if (innerIndexAndName != null) {
                val (innerIndex, innerName) = innerIndexAndName
                if (innerIndex in 1..999) {
                    if (innerName != null) {
                        // add local classes to make the local class index correct
                        repeat(innerIndex - 1) { i ->
                            append(indent)
                            append("static void method")
                            append(i)
                            append("() {\n")
                            append(indent)
                            append("   class ")
                            append(innerName)
                            append(" {}\n")
                            append(indent)
                            append("}\n")
                        }
                    } else {
                        // add anonymous classes to make the anonymous class index correct
                        repeat(innerIndex - 1) { i ->
                            append(indent)
                            append("Object inner")
                            append(i)
                            append(" = new Object() {};\n")
                        }
                    }
                }

                append(indent)
                if (innerName != null) {
                    append("static void method")
                    append(innerIndex)
                    append("() {\n")
                    closingBraces += "}"
                    indent += "   "
                    append(indent)
                    append("class ")
                    append(innerName)
                    if (index == innerClasses.lastIndex) {
                        append("<T>")
                    }
                    append(" {\n")
                    closingBraces += "}"
                } else {
                    append("Object inner")
                    append(innerIndex)
                    append(" = new ")
                    if (index == innerClasses.lastIndex) {
                        val superName = superName ?: "java/lang/Object"
                        append(superName.replace('/', '.').replace('$', '.'))
                    } else {
                        append("Object")
                    }
                    append("() {\n")
                    closingBraces += ");"
                }
            } else {
                append(indent)
                if (index != innerClasses.lastIndex || hasAccess(Opcodes.ACC_STATIC)) {
                    append("static ")
                }
                append("public class ")
                append(innerClass)
                if (index == innerClasses.lastIndex) {
                    append("<T>")
                }
                append(" {\n")
                closingBraces += "}"
            }
            indent += "   "
        }
        append(body.prependIndent(indent))
        for (i in closingBraces.lastIndex downTo 0) {
            append("\n")
            append("   ".repeat(i))
            append(closingBraces[i])
        }
    }
    val file = PsiFileFactory.getInstance(project).createFileFromText(
        "$outerClassSimpleName.java",
        JavaFileType.INSTANCE,
        text,
    ) as? PsiJavaFile ?: return null

    var clazz = file.classes.firstOrNull() ?: return null

    // associate the class with the real stub class, if it exists
    (
        JavaPsiFacade.getInstance(project).findClass(
            outerClassName.replace('/', '.'),
            GlobalSearchScope.allScope(project),
        ) as? PsiCompiledElement
        )?.let { originalClass ->
        clazz.putUserData(ClsElementImpl.COMPILED_ELEMENT, originalClass)
    }

    // find innermost PsiClass
    while (true) {
        clazz = clazz.innerClasses.firstOrNull()
            ?: clazz.anonymousClasses.lastOrNull()
            ?: clazz.localClasses.lastOrNull()
            ?: break
    }

    // add type parameters from class signature
    val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
    val typeParams = this.signature?.let { signature ->
        val sigToPsi = SignatureToPsi(elementFactory, null)
        SignatureReader(signature).accept(sigToPsi)
        sigToPsi.formalTypeParameters
    }

    if (typeParams == null || typeParams.typeParameters.isEmpty()) {
        clazz.typeParameterList?.replace(elementFactory.createTypeParameterList())
    } else {
        clazz.typeParameterList?.replace(typeParams)
    }

    return clazz
}

fun <T> ClassNode.cached(project: Project, vararg dependencies: Any, compute: (ClassNode) -> T): T {
    val unsafeClass = UnsafeCachedValueCapture(this)
    return findStubClass(project)?.cached(*dependencies) {
        compute(unsafeClass.value)
    } ?: compute(this)
}

/**
 * Finds the stub `PsiClass` for this class node (or the source code element if this is from a source file in the
 * module)
 */
fun ClassNode.findStubClass(project: Project): PsiClass? {
    return findQualifiedClass(project, name.replace('/', '.'))
}

/**
 * Attempts to find the most readable source code for this class. Checks the following locations in this order:
 * - Library sources
 * - Decompiled sources (if `canDecompile` is set to true)
 * - Stub file (which may be the source file if the source file is part of the module)
 *
 * The `canDecompile` parameter should only be set to true if this was triggered by a user action, as decompilation can
 * be slow.
 */
fun ClassNode.findSourceClass(project: Project, scope: GlobalSearchScope, canDecompile: Boolean = false): PsiClass? {
    return findQualifiedClass(name.replace('/', '.')) { name ->
        val stubClass = JavaPsiFacade.getInstance(project).findClass(name, scope) ?: return@findQualifiedClass null
        val stubFile = stubClass.containingFile ?: return@findQualifiedClass null
        val classFile = stubFile.virtualFile
        if (classFile != null) {
            val sourceFile = JavaEditorFileSwapper.findSourceFile(project, classFile)
            if (sourceFile != null) {
                val sourceClass = (PsiManager.getInstance(project).findFile(sourceFile) as? PsiJavaFile)
                    ?.classes?.firstOrNull()
                if (sourceClass != null) {
                    return@findQualifiedClass sourceClass
                }
            }
        }
        if (canDecompile) {
            ((stubFile as? PsiCompiledFile)?.decompiledPsiFile as? PsiJavaFile)?.classes?.firstOrNull()
        } else {
            stubClass
        }
    }
}

fun ClassNode.findFieldByName(name: String): FieldNode? {
    return fields?.firstOrNull { it.name == name }
}

fun ClassNode.findFields(ref: MemberMatcher): Sequence<FieldNode> {
    return fields?.asSequence()?.filter { ref.matchField(it, this) } ?: emptySequence()
}

fun ClassNode.findField(ref: MemberMatcher): FieldNode? {
    return findFields(ref).firstOrNull()
}

fun ClassNode.findMethods(ref: MixinSelector): Sequence<MethodNode> {
    val maxMatches = ref.quantifier.max(Quantifier.Context.MEMBER)
    return methods?.asSequence()?.filter { ref.matchMethod(it, this) }?.take(maxMatches).orEmpty()
}

fun ClassNode.findMethod(ref: MemberReference): MethodNode? {
    return methods?.asSequence()?.firstOrNull { ref.matchMethod(it, this) }
}

private fun makeFakeClass(name: String): ClassNode {
    val clazz = ClassNode()
    clazz.name = name
    clazz.access = Opcodes.ACC_PUBLIC
    clazz.superName = "java/lang/Object"
    return clazz
}

private fun addConstructorToFakeClass(clazz: ClassNode) {
    if (clazz.hasAccess(Opcodes.ACC_INTERFACE)) {
        return
    }
    var methods = clazz.methods
    if (methods == null) {
        methods = mutableListOf()
        clazz.methods = methods
    }
    var ctor = methods.firstOrNull { it.isConstructor }
    if (ctor == null) {
        ctor = MethodNode()
        ctor.access = Opcodes.ACC_PUBLIC
        ctor.name = "<init>"
        ctor.desc = "()V"
        methods.add(ctor)
    }
    var insns = ctor.instructions
    if (insns == null) {
        insns = InsnList()
        val superName = clazz.superName
        if (superName != null) {
            insns.add(VarInsnNode(Opcodes.ALOAD, 0))
            insns.add(MethodInsnNode(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false))
            ctor.maxStack = 1
        }
        insns.add(InsnNode(Opcodes.RETURN))
        ctor.instructions = insns
    }
}

// FieldNode

fun FieldNode.hasAccess(flag: Int) = hasAccess(this.access, flag)

@PsiUtil.AccessLevel
val FieldNode.accessLevel
    get() = accessLevelFromFlags(this.access)

fun FieldNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

val FieldNode.memberReference
    get() = MemberReference(this.name, this.desc)

fun FieldNode.getGenericType(
    clazz: ClassNode,
    project: Project,
): PsiType {
    if (this.signature != null) {
        return findOrConstructSourceField(clazz, project, canDecompile = false).type
    }
    val elementFactory = JavaPsiFacade.getElementFactory(project)
    return Type.getType(this.desc).toPsiType(elementFactory)
}

fun <T> FieldNode.cached(
    clazz: ClassNode,
    project: Project,
    vararg dependencies: Any,
    compute: (ClassNode, FieldNode) -> T,
): T {
    val unsafeClass = UnsafeCachedValueCapture(clazz)
    val unsafeField = UnsafeCachedValueCapture(this)
    return findStubField(clazz, project)?.cached(*dependencies) {
        compute(unsafeClass.value, unsafeField.value)
    } ?: compute(clazz, this)
}

fun FieldNode.findStubField(clazz: ClassNode, project: Project): PsiField? {
    return clazz.findStubClass(project)?.findField(memberReference)
}

/**
 * Attempts to find the source field using [findSourceField], and constructs one if it couldn't be found.
 *
 * The returned field will be inside a valid `PsiClass` inside a valid `PsiJavaFile`, if the `clazz` parameter is given.
 */
fun FieldNode.findOrConstructSourceField(
    clazz: ClassNode?,
    project: Project,
    scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
    canDecompile: Boolean = false,
): PsiField {
    clazz?.let { findSourceField(it, project, scope, canDecompile = canDecompile) }?.let { return it }

    val elementFactory = JavaPsiFacade.getInstance(project).elementFactory

    val containingClass = clazz?.constructClass(project, "int foo;")

    val signature = this.signature
    val type = if (signature != null) {
        val sigToPsi = SignatureToPsi(elementFactory, containingClass)
        SignatureReader(signature).acceptType(sigToPsi)
        sigToPsi.type
    } else {
        Type.getType(this.desc).toPsiType(elementFactory)
    }
    val psiField = elementFactory.createField(
        this.name.toJavaIdentifier(),
        type,
    )
    psiField.realName = this.name
    val modifierList = psiField.modifierList!!
    setBaseModifierProperties(modifierList, access)
    modifierList.setModifierProperty(PsiModifier.VOLATILE, hasAccess(Opcodes.ACC_VOLATILE))
    modifierList.setModifierProperty(PsiModifier.TRANSIENT, hasAccess(Opcodes.ACC_TRANSIENT))
    return containingClass
        ?.findFieldByName("foo", false)
        ?.replace(psiField) as? PsiField
        ?: psiField
}

/**
 * Attempts to find the most readable source field for this field, using the same technique as described in
 * [findSourceClass]
 */
fun FieldNode.findSourceField(
    clazz: ClassNode,
    project: Project,
    scope: GlobalSearchScope,
    canDecompile: Boolean = false,
): PsiField? {
    return clazz.findSourceClass(project, scope, canDecompile)?.findField(memberReference)
}

/**
 * Constructs a fake field node which could have been reached via this field instruction
 */
fun FieldInsnNode.fakeResolve(): ClassAndFieldNode {
    val clazz = makeFakeClass(owner)
    val field = FieldNode(Opcodes.ACC_PUBLIC, name, desc, null, null)
    if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
        field.access = field.access or Opcodes.ACC_STATIC
    }
    clazz.fields = mutableListOf(field)
    addConstructorToFakeClass(clazz)
    return ClassAndFieldNode(clazz, field)
}

// MethodNode

fun MethodNode.hasAccess(flag: Int) = hasAccess(this.access, flag)

@PsiUtil.AccessLevel
val MethodNode.accessLevel
    get() = accessLevelFromFlags(this.access)

fun MethodNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

val MethodNode.memberReference
    get() = MemberReference(this.name, this.desc)

fun MethodNode.getGenericSignature(clazz: ClassNode, project: Project): Pair<PsiType, List<PsiType>> {
    var pair: Pair<PsiType, List<PsiType>>? = null
    if (this.signature != null) {
        val sourceMethod = findOrConstructSourceMethod(clazz, project, canDecompile = false)
        sourceMethod.returnType?.let { returnType ->
            pair = returnType to sourceMethod.parameterList.parameters.map { it.type }
        }
    }

    if (pair == null) {
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        pair = Type.getReturnType(this.desc).toPsiType(elementFactory) to
            Type.getArgumentTypes(this.desc).map { it.toPsiType(elementFactory) }
    }
    var ret = pair!!

    val lastType = ret.second.lastOrNull()
    if (hasAccess(Opcodes.ACC_VARARGS) && lastType is PsiArrayType) {
        ret = ret.first to (ret.second.dropLast(1) + PsiEllipsisType(lastType.componentType))
    }

    return ret
}

fun MethodNode.getGenericReturnType(clazz: ClassNode, project: Project): PsiType {
    return getGenericSignature(clazz, project).first
}

fun MethodNode.getGenericParameterTypes(clazz: ClassNode, project: Project): List<PsiType> {
    return getGenericSignature(clazz, project).second
}

val MethodNode.isConstructor
    get() = this.name == "<init>"

val MethodNode.isClinit
    get() = this.name == "<clinit>"

/**
 * Finds the `this()` or `super()` call in this method node, assuming it is a constructor
 */
fun MethodNode.findDelegateConstructorCall(): MethodInsnNode? {
    val insns = instructions ?: return null
    var superCtorCall = insns.first
    var newCount = 0
    while (superCtorCall != null) {
        if (superCtorCall.opcode == Opcodes.NEW) {
            newCount++
        } else if (superCtorCall.opcode == Opcodes.INVOKESPECIAL) {
            superCtorCall as MethodInsnNode
            if (superCtorCall.name == "<init>") {
                if (newCount == 0) {
                    return superCtorCall
                } else {
                    newCount--
                }
            }
        }
        superCtorCall = superCtorCall.next
    }
    return null
}

private fun findContainingMethod(clazz: ClassNode, lambdaMethod: MethodNode): Pair<MethodNode, SourceCodeLocationInfo>? {
    if (!lambdaMethod.hasAccess(Opcodes.ACC_SYNTHETIC)) {
        return null
    }
    clazz.methods?.forEach { method ->
        var lambdaCount = 0
        var lineNumber: Int? = null
        val lambdaCountPerLine = mutableMapOf<Int, Int>()
        method.instructions?.iterator()?.forEach nextInsn@{ insn ->
            if (insn is LineNumberNode) {
                lineNumber = insn.line
            }
            if (insn !is InvokeDynamicInsnNode) return@nextInsn
            if (insn.bsm.owner != "java/lang/invoke/LambdaMetafactory") return@nextInsn
            val invokedMethod = when (insn.bsm.name) {
                "metafactory" -> {
                    if (insn.bsmArgs.size < 3) return@nextInsn
                    insn.bsmArgs[1] as? Handle ?: return@nextInsn
                }
                "altMetafactory" -> {
                    if (insn.bsmArgs.size < 2) return@nextInsn
                    val extraArgs = insn.bsmArgs[0] as? Array<*> ?: return@nextInsn
                    if (extraArgs.size < 2) return@nextInsn
                    extraArgs[1] as? Handle ?: return@nextInsn
                }
                else -> return@nextInsn
            }

            // check if this lambda generated a synthetic method
            if (invokedMethod.owner != clazz.name) return@nextInsn
            val invokedMethodNode = clazz.findMethod(MemberReference(invokedMethod.name, invokedMethod.desc))
            if (invokedMethodNode == null || !invokedMethodNode.hasAccess(Opcodes.ACC_SYNTHETIC)) {
                return@nextInsn
            }

            lambdaCount++
            val lambdaCountThisLine =
                lineNumber?.let { lambdaCountPerLine.merge(it, 1, Int::plus) } ?: lambdaCount

            if (invokedMethod.name == lambdaMethod.name && invokedMethod.desc == lambdaMethod.desc) {
                val locationInfo =
                    SourceCodeLocationInfo(lambdaCount - 1, lineNumber, lambdaCountThisLine - 1)
                return@findContainingMethod method to locationInfo
            }
        }
    }

    return null
}

private fun findAssociatedLambda(project: Project, scope: GlobalSearchScope, clazz: ClassNode, lambdaMethod: MethodNode): PsiElement? {
    return RecursionManager.doPreventingRecursion(lambdaMethod, false) {
        val pair = findContainingMethod(clazz, lambdaMethod) ?: return@doPreventingRecursion null
        val (containingMethod, locationInfo) = pair
        val containingBodyElements = findAssociatedLambda(project, scope, clazz, containingMethod)?.let(::listOf)
            ?: containingMethod.findBodyElements(clazz, project, scope).ifEmpty { return@doPreventingRecursion null }

        val psiFile = containingBodyElements.first().containingFile ?: return@doPreventingRecursion null
        val matcher = locationInfo.createMatcher<PsiElement>(psiFile)
        for (bodyElement in containingBodyElements) {
            bodyElement.accept(
                object : JavaRecursiveElementWalkingVisitor() {
                    override fun visitAnonymousClass(aClass: PsiAnonymousClass) {
                        // skip anonymous classes
                    }

                    override fun visitClass(aClass: PsiClass) {
                        // skip inner classes
                    }

                    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
                        if (matcher.accept(expression)) {
                            stopWalking()
                        }
                        // skip walking inside the lambda
                    }

                    override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
                        // walk inside the reference first, visits the qualifier first (it's first in the bytecode)
                        super.visitMethodReferenceExpression(expression)

                        if (expression.hasSyntheticMethod(clazz.version)) {
                            if (matcher.accept(expression)) {
                                stopWalking()
                            }
                        }
                    }
                },
            )
        }

        matcher.result
    }
}

fun <T> MethodNode.cached(
    clazz: ClassNode,
    project: Project,
    vararg dependencies: Array<Any>,
    compute: (ClassNode, MethodNode) -> T,
): T {
    val unsafeClass = UnsafeCachedValueCapture(clazz)
    val unsafeMethod = UnsafeCachedValueCapture(this)
    return findStubMethod(clazz, project)?.cached(*dependencies) {
        compute(unsafeClass.value, unsafeMethod.value)
    } ?: compute(clazz, this)
}

fun MethodNode.findStubMethod(clazz: ClassNode, project: Project): PsiMethod? {
    return clazz.findStubClass(project)?.findMethods(memberReference)?.firstOrNull()
}

private fun MethodNode.getOffset(clazz: ClassNode?): Int {
    return if (this.isConstructor) {
        when {
            clazz?.hasAccess(Opcodes.ACC_ENUM) == true -> 2
            clazz?.outerClass != null && !clazz.hasAccess(Opcodes.ACC_STATIC) -> 1
            else -> 0
        }
    } else {
        0
    }
}

fun MethodNode.getParameter(clazz: ClassNode, index: Int, parameterList: PsiParameterList): PsiParameter? {
    return parameterList.parameters.getOrNull(index - getOffset(clazz))
}

/**
 * Attempts to find the source method using [findSourceElement]. If this fails, or if the result is not a `PsiMethod`,
 * then a new source method is constructed, possibly copying the body of the found source element.
 *
 * The returned method will be inside a valid `PsiClass` inside a valid `PsiJavaFile`, if the `clazz` parameter is
 * given.
 */
fun MethodNode.findOrConstructSourceMethod(
    clazz: ClassNode?,
    project: Project,
    scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
    canDecompile: Boolean = false,
): PsiMethod {
    val sourceElement = clazz?.let { findSourceElement(it, project, scope, canDecompile = canDecompile) }
    if (sourceElement is PsiMethod) {
        return sourceElement
    }

    val psiClass = clazz?.constructClass(project, "void foo(){}")

    val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
    val methodText = buildString {
        append("public <T> ")
        val returnType = Type.getReturnType(this@findOrConstructSourceMethod.desc)
        if (isConstructor) {
            var name = "_init_"
            val simpleName = clazz?.name?.substringAfterLast('/')
            if (simpleName != null) {
                name = simpleName.substringAfterLast('$')
                while (name.isNotEmpty() && !name[0].isJavaIdentifierStart()) {
                    val dollarIndex = simpleName.lastIndexOf('$', simpleName.length - name.length - 2)
                    if (dollarIndex == -1) {
                        name = simpleName
                        break
                    }
                    name = simpleName.substring(dollarIndex + 1)
                }
            }
            append(name)
        } else {
            append(returnType.canonicalName)
            append(' ')
            append(this@findOrConstructSourceMethod.name.toJavaIdentifier())
        }
        append('(')
        val params = Type.getArgumentTypes(this@findOrConstructSourceMethod.desc)
        for ((index, param) in params.withIndex()) {
            if (index != 0) {
                append(", ")
            }
            var typeName = param.canonicalName
            if (index == params.size - 1 && hasAccess(Opcodes.ACC_VARARGS) && typeName.endsWith("[]")) {
                typeName = typeName.replaceRange(typeName.length - 2, typeName.length, "...")
            }
            append(typeName)
            append(" par")
            append(index + 1)
        }
        append(')')
        if (hasAccess(Opcodes.ACC_ABSTRACT) || hasAccess(Opcodes.ACC_NATIVE)) {
            append(';')
        } else {
            append(" { /* compiled code */ ")
            if (returnType.sort != Type.VOID) {
                append("return ")
                if (returnType.sort == Type.OBJECT || returnType.sort == Type.ARRAY) {
                    append("null")
                } else {
                    append('0')
                }
                append("; ")
            }
            append('}')
        }
    }
    val tempMethod = elementFactory.createMethodFromText(methodText, psiClass)
    // put the method inside the class, if given
    val psiMethod = psiClass
        ?.findMethodsByName("foo", false)
        ?.firstOrNull()
        ?.replace(tempMethod) as? PsiMethod
        ?: tempMethod
    psiMethod.realName = name

    // replace signature first so that subsequent generics resolution can work
    val typeParams = this.signature?.let { signature ->
        val sigToPsi = SignatureToPsi(elementFactory, psiClass)
        SignatureReader(signature).accept(sigToPsi)
        sigToPsi.formalTypeParameters
    }
    if (typeParams == null || typeParams.typeParameters.isEmpty()) {
        psiMethod.typeParameterList?.replace(elementFactory.createTypeParameterList())
    } else {
        psiMethod.typeParameterList?.replace(typeParams)
    }

    // replace other generics
    this.signature?.let { signature ->
        val sigToPsi = SignatureToPsi(elementFactory, psiMethod)
        SignatureReader(signature).accept(sigToPsi)

        val offset = this.getOffset(clazz)

        for ((index, parameterType) in sigToPsi.parameterTypes.withIndex()) {
            val parameter = psiMethod.parameterList.getParameter(index + offset) ?: continue
            if (!parameter.type.isErasureEquivalentTo(parameterType)) continue
            // make sure to respect varargs
            val actualType = if (parameter.type is PsiEllipsisType && parameterType is PsiArrayType) {
                PsiEllipsisType(parameterType.componentType, parameterType.annotations)
            } else {
                parameterType
            }

            val typeElement = elementFactory.createTypeElement(actualType)
            parameter.typeElement?.replace(typeElement)
        }

        sigToPsi.returnType?.let { returnType ->
            psiMethod.returnTypeElement?.replace(elementFactory.createTypeElement(returnType))
        }

        for ((index, exceptionType) in sigToPsi.exceptionTypes.withIndex()) {
            val throwsType = psiMethod.throwsList.referenceElements.getOrNull(index) ?: continue
            if (exceptionType !is PsiClassType) continue
            throwsType.replace(elementFactory.createReferenceElementByType(exceptionType))
        }
    }

    // the body of the method may have still been in the source method if it wasn't actually a method
    when (sourceElement) {
        is PsiLambdaExpression -> {
            val copy = sourceElement.copy() as PsiLambdaExpression
            psiMethod.body?.replace(CommonJavaRefactoringUtil.expandExpressionLambdaToCodeBlock(copy))
        }
        is PsiMethodReferenceExpression -> {
            LambdaRefactoringUtil.createLambda(sourceElement, true)?.let {
                psiMethod.body?.replace(CommonJavaRefactoringUtil.expandExpressionLambdaToCodeBlock(it))
            }
        }
    }

    val exceptions = exceptions
    if (exceptions != null) {
        psiMethod.throwsList.replace(
            elementFactory.createReferenceList(
                exceptions.mapToArray { elementFactory.createReferenceFromText(it.replace('/', '.'), null) },
            ),
        )
    }

    val modifierList = psiMethod.modifierList
    setBaseModifierProperties(modifierList, access)
    modifierList.setModifierProperty(PsiModifier.SYNCHRONIZED, hasAccess(Opcodes.ACC_SYNCHRONIZED))
    modifierList.setModifierProperty(PsiModifier.NATIVE, hasAccess(Opcodes.ACC_NATIVE))

    return psiMethod
}

private fun setBaseModifierProperties(modifierList: PsiModifierList, access: Int) {
    modifierList.setModifierProperty(PsiModifier.PUBLIC, hasAccess(access, Opcodes.ACC_PUBLIC))
    modifierList.setModifierProperty(PsiModifier.PROTECTED, hasAccess(access, Opcodes.ACC_PROTECTED))
    modifierList.setModifierProperty(PsiModifier.PRIVATE, hasAccess(access, Opcodes.ACC_PRIVATE))
    modifierList.setModifierProperty(PsiModifier.STATIC, hasAccess(access, Opcodes.ACC_STATIC))
    modifierList.setModifierProperty(PsiModifier.FINAL, hasAccess(access, Opcodes.ACC_FINAL))
}

/**
 * Attempts to find the most readable source element corresponding to this method, using the same priorities as
 * [findSourceClass]. If this method is synthetic and corresponds to a lambda expression or method reference, attempts
 * to find the associated lambda expression or method reference. If the class source couldn't be found and only a stub
 * tree is located, then lambdas cannot be searched for as that requires looking inside method bodies.
 */
fun MethodNode.findSourceElement(
    clazz: ClassNode,
    project: Project,
    scope: GlobalSearchScope,
    canDecompile: Boolean = false,
): PsiElement? {
    val psiClass = clazz.findSourceClass(project, scope, canDecompile) ?: return null
    if (isClinit) {
        return psiClass.childrenOfType<PsiClassInitializer>().firstOrNull { it.hasModifierProperty(PsiModifier.STATIC) }
            ?: psiClass
    }
    psiClass.findMethods(memberReference).firstOrNull()?.let { return it }
    if (psiClass is PsiCompiledElement) {
        // don't walk into stub compiled elements to look for lambdas
        return null
    }
    return findAssociatedLambda(project, scope, clazz, this)
}

fun MethodNode.findBodyElements(clazz: ClassNode, project: Project, scope: GlobalSearchScope): List<PsiElement> {
    if (isClinit) {
        val psiClass = clazz.findSourceClass(project, scope, canDecompile = true) ?: return emptyList()
        val result = mutableListOf<PsiElement>()
        for (element in psiClass.children) {
            when (element) {
                is PsiEnumConstant -> element.argumentList?.expressions?.let { result += it }
                is PsiField -> {
                    if (element.hasModifierProperty(PsiModifier.STATIC)) {
                        element.initializer?.let { result += it }
                    }
                }
                is PsiClassInitializer -> {
                    if (element.hasModifierProperty(PsiModifier.STATIC)) {
                        result += element.body
                    }
                }
            }
        }
        return result
    }

    val sourceMethod = findSourceElement(clazz, project, scope, canDecompile = true) ?: return emptyList()

    if (isConstructor && findDelegateConstructorCall()?.owner != clazz.name && sourceMethod is PsiMethod) {
        val result = mutableListOf<PsiElement>()
        val body = sourceMethod.body
        if (body != null) {
            val children = body.children
            val superCtorIndex = children.indexOfFirst {
                it is PsiMethodCallExpression && it.methodExpression.text == JavaKeywords.SUPER
            }
            result += children.take(superCtorIndex + 1)
            sourceMethod.containingClass?.children?.forEach { element ->
                when (element) {
                    is PsiField -> {
                        if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                            element.initializer?.let { result += it }
                        }
                    }
                    is PsiClassInitializer -> {
                        if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                            result += element.body
                        }
                    }
                }
            }
            result += children.drop(superCtorIndex + 1)
            return result
        }
    }

    val body = when (sourceMethod) {
        is PsiParameterListOwner -> sourceMethod.body
        else -> null
    }

    return listOfNotNull(body)
}

/**
 * Constructs a fake method node which could have been reached via this method instruction
 */
fun MethodInsnNode.fakeResolve(): ClassAndMethodNode {
    val clazz = makeFakeClass(owner)
    if (itf) {
        clazz.access = clazz.access or Opcodes.ACC_INTERFACE
    }
    val method = MethodNode()
    method.access = Opcodes.ACC_PUBLIC
    if (opcode == Opcodes.INVOKESTATIC) {
        method.access = method.access or Opcodes.ACC_STATIC
    }
    method.name = name
    method.desc = desc
    clazz.methods = mutableListOf(method)
    addConstructorToFakeClass(clazz)
    return ClassAndMethodNode(clazz, method)
}

// AbstractInsnNode

val AbstractInsnNode.nextRealInsn: AbstractInsnNode?
    get() {
        var insn = next
        while (insn != null && insn.opcode < 0) {
            insn = insn.next
        }
        return insn
    }

val AbstractInsnNode.previousRealInsn: AbstractInsnNode?
    get() {
        var insn = previous
        while (insn != null && insn.opcode < 0) {
            insn = insn.previous
        }
        return insn
    }

// Textifier

fun ClassNode.textify(): String {
    val sw = StringWriter()
    accept(TraceClassVisitor(PrintWriter(sw)))
    return sw.toString().replaceIndent().trimEnd()
}

fun FieldNode.textify(): String {
    val cv = TraceClassVisitor(null)
    accept(cv)
    val sw = StringWriter()
    cv.p.print(PrintWriter(sw))
    return sw.toString().replaceIndent().trimEnd()
}

fun MethodNode.textify(): String {
    val cv = TraceClassVisitor(null)
    accept(cv)
    val sw = StringWriter()
    cv.p.print(PrintWriter(sw))
    return sw.toString().replaceIndent().trimEnd()
}

fun AnnotationNode.textify(): String {
    val textifier = Textifier()
    accept(TraceAnnotationVisitor(textifier))
    val sw = StringWriter()
    textifier.print(PrintWriter(sw))
    return sw.toString().replaceIndent().trimEnd()
}

fun AbstractInsnNode.textify(): String {
    val mv = TraceMethodVisitor(Textifier())
    accept(mv)
    val sw = StringWriter()
    mv.p.print(PrintWriter(sw))
    return sw.toString().replaceIndent().trimEnd()
}
