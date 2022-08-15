/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.anonymousElements
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.hasSyntheticMethod
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
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
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.LambdaRefactoringUtil
import com.intellij.refactoring.util.RefactoringUtil
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

private val LOGGER = Logger.getInstance("AsmUtil")

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
    return elementFactory.createTypeFromText(className.replace('$', '.'), context)
}

private fun hasAccess(access: Int, flag: Int) = (access and flag) != 0

// ClassNode

fun ClassNode.hasAccess(flag: Int) = hasAccess(this.access, flag)

fun ClassNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

fun internalNameToShortName(internalName: String) = internalName.substringAfterLast('/').replace('$', '.')

val ClassNode.shortName
    get() = internalNameToShortName(name)

private val LOAD_CLASS_FILE_BYTES: Method? = runCatching {
    com.intellij.byteCodeViewer.ByteCodeViewerManager::class.java
        .getDeclaredMethod("loadClassFileBytes", PsiClass::class.java)
        .let { it.isAccessible = true; it }
}.getOrNull()

/**
 * Tries to find the bytecode for the class for the given qualified name.
 *
 * ### Implementation note:
 * First attempts to resolve the class using [findQualifiedClass]. This may fail in the case of anonymous classes, which
 * don't exist inside `PsiCompiledElement`s, so it then creates a fake `PsiClass` based on the qualified name and
 * attempts to resolve it from that.
 */
fun findClassNodeByQualifiedName(project: Project, module: Module?, fqn: String): ClassNode? {
    val psiClass = findQualifiedClass(project, fqn)
    if (psiClass != null) {
        return findClassNodeByPsiClass(psiClass, module)
    }

    // try to find it by a fake one
    val fakeClassNode = ClassNode()
    fakeClassNode.name = fqn.replace('.', '/')
    val fakePsiClass = fakeClassNode.constructClass(project, "") ?: return null
    return findClassNodeByPsiClass(fakePsiClass, module)
}

fun findClassNodeByPsiClass(psiClass: PsiClass, module: Module? = psiClass.findModule()): ClassNode? {
    return try {
        val bytes = LOAD_CLASS_FILE_BYTES?.invoke(null, psiClass) as? ByteArray
        if (bytes == null) {
            // find compiler output
            if (module == null) return null
            val fqn = psiClass.fullQualifiedName ?: return null
            var parentDir = CompilerModuleExtension.getInstance(module)?.compilerOutputPath ?: return null
            val packageName = fqn.substringBeforeLast('.', "")
            if (packageName.isNotEmpty()) {
                for (dir in packageName.split('.')) {
                    parentDir = parentDir.findChild(dir) ?: return null
                }
            }
            val classFile = parentDir.findChild("${fqn.substringAfterLast('.')}.class") ?: return null
            val node = ClassNode()
            classFile.inputStream.use { ClassReader(it).accept(node, 0) }
            node
        } else {
            val node = ClassNode()
            ClassReader(bytes).accept(node, 0)
            node
        }
    } catch (e: Throwable) {
        val actualThrowable = if (e is InvocationTargetException) e.cause ?: e else e
        if (actualThrowable is ProcessCanceledException) {
            throw actualThrowable
        }
        val message = actualThrowable.message
        // TODO: display an error to the user?
        if (message == null || !message.contains("Unsupported class file major version")) {
            LOGGER.error(actualThrowable)
        }
        null
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
        for ((index, innerClass) in innerClasses.withIndex()) {
            val anonymousIndex = innerClass.toIntOrNull()
            if (anonymousIndex != null) {
                // add anonymous classes make the anonymous class index correct
                if (anonymousIndex in 1..999) {
                    repeat(anonymousIndex - 1) { i ->
                        append(indent)
                        append("Object inner")
                        append(i)
                        append(" = new Object() {};\n")
                    }
                }
                append(indent)
                append("Object inner")
                append(anonymousIndex)
                append(" = new ")
                if (index == innerClasses.lastIndex) {
                    val superName = superName ?: "java/lang/Object"
                    append(superName.replace('/', '.').replace('$', '.'))
                } else {
                    append("Object")
                }
                append("() {} {\n")
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
            }
            indent += "   "
        }
        append(body.prependIndent(indent))
        repeat(innerClasses.size + 1) { i ->
            append("\n")
            append("   ".repeat(innerClasses.size - i))
            append("}")
            // append ; after anonymous class declarations
            if (i < innerClasses.size && innerClasses[innerClasses.size - 1 - i].toIntOrNull() != null) {
                append(";")
            }
        }
    }
    val file = PsiFileFactory.getInstance(project).createFileFromText(
        "$outerClassSimpleName.java",
        JavaFileType.INSTANCE,
        text
    ) as? PsiJavaFile ?: return null

    var clazz = file.classes.firstOrNull() ?: return null

    // associate the class with the real stub class, if it exists
    (
        JavaPsiFacade.getInstance(project).findClass(
            outerClassName.replace('/', '.'),
            GlobalSearchScope.allScope(project)
        ) as? PsiCompiledElement
        )?.let { originalClass ->
        clazz.putUserData(ClsElementImpl.COMPILED_ELEMENT, originalClass)
    }

    // find innermost PsiClass
    while (true) {
        clazz = clazz.innerClasses.firstOrNull()
            ?: clazz.anonymousElements.lastOrNull { it !== clazz && it is PsiClass } as? PsiClass
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

inline fun <T> ClassNode.cached(project: Project, vararg dependencies: Any, crossinline compute: () -> T): T {
    return findStubClass(project)?.cached(*dependencies, compute = compute) ?: compute()
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

fun ClassNode.findFields(ref: MixinSelector): Sequence<FieldNode> {
    return fields?.asSequence()?.filter { ref.matchField(it, this) } ?: emptySequence()
}

fun ClassNode.findField(ref: MixinSelector): FieldNode? {
    return findFields(ref).firstOrNull()
}

fun ClassNode.findMethods(ref: MixinSelector): Sequence<MethodNode> {
    return methods?.asSequence()?.filter { ref.matchMethod(it, this) } ?: emptySequence()
}

fun ClassNode.findMethod(ref: MixinSelector): MethodNode? {
    return findMethods(ref).firstOrNull()
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
    project: Project
): PsiType {
    if (this.signature != null) {
        return findOrConstructSourceField(clazz, project, canDecompile = false).type
    }
    val elementFactory = JavaPsiFacade.getElementFactory(project)
    return Type.getType(this.desc).toPsiType(elementFactory)
}

inline fun <T> FieldNode.cached(
    clazz: ClassNode,
    project: Project,
    vararg dependencies: Any,
    crossinline compute: () -> T
): T {
    return findStubField(clazz, project)?.cached(*dependencies, compute = compute) ?: compute()
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
    canDecompile: Boolean = false
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
        type
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
    canDecompile: Boolean = false
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

private fun findContainingMethod(clazz: ClassNode, lambdaMethod: MethodNode): Pair<MethodNode, Int>? {
    if (!lambdaMethod.hasAccess(Opcodes.ACC_SYNTHETIC)) {
        return null
    }
    clazz.methods?.forEach { method ->
        var lambdaCount = 0
        method.instructions?.iterator()?.forEach nextInsn@{ insn ->
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

            if (invokedMethod.name == lambdaMethod.name && invokedMethod.desc == lambdaMethod.desc) {
                return@findContainingMethod method to (lambdaCount - 1)
            }
        }
    }

    return null
}

private fun findAssociatedLambda(psiClass: PsiClass, clazz: ClassNode, lambdaMethod: MethodNode): PsiElement? {
    return RecursionManager.doPreventingRecursion(lambdaMethod, false) {
        val pair = findContainingMethod(clazz, lambdaMethod) ?: return@doPreventingRecursion null
        val (containingMethod, index) = pair
        val parent = findAssociatedLambda(psiClass, clazz, containingMethod)
            ?: psiClass.findMethods(containingMethod.memberReference).firstOrNull()
            ?: return@doPreventingRecursion null
        var i = 0
        var result: PsiElement? = null
        parent.accept(
            object : JavaRecursiveElementWalkingVisitor() {
                override fun visitAnonymousClass(aClass: PsiAnonymousClass?) {
                    // skip anonymous classes
                }

                override fun visitClass(aClass: PsiClass?) {
                    // skip inner classes
                }

                override fun visitLambdaExpression(expression: PsiLambdaExpression?) {
                    if (i++ == index) {
                        result = expression
                        stopWalking()
                    }
                    // skip walking inside the lambda
                }

                override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
                    // walk inside the reference first, visits the qualifier first (it's first in the bytecode)
                    super.visitMethodReferenceExpression(expression)

                    if (expression.hasSyntheticMethod) {
                        if (i++ == index) {
                            result = expression
                            stopWalking()
                        }
                    }
                }
            }
        )
        result
    }
}

inline fun <T> MethodNode.cached(
    clazz: ClassNode,
    project: Project,
    vararg dependencies: Array<Any>,
    crossinline compute: () -> T
): T {
    return findStubMethod(clazz, project)?.cached(*dependencies, compute = compute) ?: compute()
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
    canDecompile: Boolean = false
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
                while (!name[0].isJavaIdentifierStart()) {
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
            append(returnType.className.replace('$', '.'))
            append(' ')
            append(this@findOrConstructSourceMethod.name.toJavaIdentifier())
        }
        append('(')
        val params = Type.getArgumentTypes(this@findOrConstructSourceMethod.desc)
        for ((index, param) in params.withIndex()) {
            if (index != 0) {
                append(", ")
            }
            var typeName = param.className.replace('$', '.')
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
            psiMethod.body?.replace(RefactoringUtil.expandExpressionLambdaToCodeBlock(copy))
        }
        is PsiMethodReferenceExpression -> {
            LambdaRefactoringUtil.createLambda(sourceElement, true)?.let {
                psiMethod.body?.replace(RefactoringUtil.expandExpressionLambdaToCodeBlock(it))
            }
        }
    }

    val exceptions = exceptions
    if (exceptions != null) {
        psiMethod.throwsList.replace(
            elementFactory.createReferenceList(
                exceptions.mapToArray { elementFactory.createReferenceFromText(it.replace('/', '.'), null) }
            )
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
    canDecompile: Boolean = false
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
    return findAssociatedLambda(psiClass, clazz, this)
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
