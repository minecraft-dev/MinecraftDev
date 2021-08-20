/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.anonymousElements
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.hasSyntheticMethod
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.LambdaRefactoringUtil
import com.intellij.refactoring.util.RefactoringUtil
import java.lang.reflect.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
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

private val MODIFIER_TO_ACCESS_FLAG = mapOf<@PsiModifier.ModifierConstant String, Int>(
    PsiModifier.PUBLIC to Opcodes.ACC_PUBLIC,
    PsiModifier.PROTECTED to Opcodes.ACC_PROTECTED,
    PsiModifier.PRIVATE to Opcodes.ACC_PRIVATE,
    PsiModifier.STATIC to Opcodes.ACC_STATIC,
    PsiModifier.ABSTRACT to Opcodes.ACC_ABSTRACT,
    PsiModifier.FINAL to Opcodes.ACC_FINAL,
    PsiModifier.NATIVE to Opcodes.ACC_NATIVE,
    PsiModifier.SYNCHRONIZED to Opcodes.ACC_SYNCHRONIZED,
    PsiModifier.STRICTFP to Opcodes.ACC_STRICT,
    PsiModifier.TRANSIENT to Opcodes.ACC_TRANSIENT,
    PsiModifier.VOLATILE to Opcodes.ACC_VOLATILE,
    PsiModifier.OPEN to Opcodes.ACC_OPEN,
    PsiModifier.TRANSITIVE to Opcodes.ACC_TRANSITIVE,
)

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

// ClassNode

fun ClassNode.hasAccess(flag: Int) = (this.access and flag) != 0

fun ClassNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

fun internalNameToShortName(internalName: String) = internalName.substringAfterLast('/').replace('$', '.')

val ClassNode.shortName
    get() = internalNameToShortName(name)

private val LOAD_CLASS_FILE_BYTES: Method? = runCatching {
    com.intellij.byteCodeViewer.ByteCodeViewerManager::class.java
        .getDeclaredMethod("loadClassFileBytes", PsiClass::class.java)
        .let { it.isAccessible = true; it }
}.getOrNull()

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
    return runCatching {
        val bytes = LOAD_CLASS_FILE_BYTES?.invoke(null, psiClass) as? ByteArray
        if (bytes == null) {
            // find compiler output
            if (module == null) return@runCatching null
            val fqn = psiClass.fullQualifiedName ?: return@runCatching null
            var parentDir = CompilerModuleExtension.getInstance(module)?.compilerOutputPath ?: return@runCatching null
            val packageName = fqn.substringBeforeLast('.', "")
            if (packageName.isNotEmpty()) {
                for (dir in packageName.split('.')) {
                    parentDir = parentDir.findChild(dir) ?: return@runCatching null
                }
            }
            val classFile = parentDir.findChild("${fqn.substringAfterLast('.')}.class") ?: return@runCatching null
            val node = ClassNode()
            ClassReader(classFile.inputStream).accept(node, 0)
            return@runCatching node
        } else {
            val node = ClassNode()
            ClassReader(bytes).accept(node, 0)
            return@runCatching node
        }
    }.getOrLogException(LOGGER)
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
            ?: return clazz
    }
}

fun ClassNode.findStubClass(project: Project): PsiClass? {
    return findQualifiedClass(project, name.replace('/', '.'))
}

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

fun ClassNode.findMethods(ref: MemberReference): Sequence<MethodNode> {
    return methods?.asSequence()
        ?.filter { it.name == ref.name && (ref.descriptor == null || it.desc == ref.descriptor) } ?: emptySequence()
}

fun ClassNode.findMethod(ref: MemberReference): MethodNode? {
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

fun FieldNode.hasAccess(flag: Int) = (this.access and flag) != 0

@PsiUtil.AccessLevel
val FieldNode.accessLevel
    get() = accessLevelFromFlags(this.access)

fun FieldNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

val FieldNode.memberReference
    get() = MemberReference(this.name, this.desc)

fun FieldNode.findStubField(clazz: ClassNode, project: Project): PsiField? {
    return clazz.findStubClass(project)?.findField(memberReference)
}

fun FieldNode.findOrConstructSourceField(
    clazz: ClassNode?,
    project: Project,
    scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
    canDecompile: Boolean = false
): PsiField {
    clazz?.let { findSourceField(it, project, scope, canDecompile = canDecompile) }?.let { return it }

    val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
    val psiField = elementFactory.createField(
        this.name.toJavaIdentifier(),
        elementFactory.createTypeFromText(Type.getType(this.desc).className, null)
    )
    psiField.realName = this.name
    val modifierList = psiField.modifierList!!
    modifierList.setModifierProperty(PsiModifier.PUBLIC, hasAccess(Opcodes.ACC_PUBLIC))
    modifierList.setModifierProperty(PsiModifier.PROTECTED, hasAccess(Opcodes.ACC_PROTECTED))
    modifierList.setModifierProperty(PsiModifier.PRIVATE, hasAccess(Opcodes.ACC_PRIVATE))
    modifierList.setModifierProperty(PsiModifier.STATIC, hasAccess(Opcodes.ACC_STATIC))
    modifierList.setModifierProperty(PsiModifier.FINAL, hasAccess(Opcodes.ACC_FINAL))
    modifierList.setModifierProperty(PsiModifier.VOLATILE, hasAccess(Opcodes.ACC_VOLATILE))
    modifierList.setModifierProperty(PsiModifier.TRANSIENT, hasAccess(Opcodes.ACC_TRANSIENT))
    return clazz
        ?.constructClass(project, "int foo;")
        ?.findFieldByName("foo", false)
        ?.replace(psiField) as? PsiField
        ?: psiField
}

fun FieldNode.findSourceField(
    clazz: ClassNode,
    project: Project,
    scope: GlobalSearchScope,
    canDecompile: Boolean = false
): PsiField? {
    return clazz.findSourceClass(project, scope, canDecompile)?.findField(memberReference)
}

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

fun MethodNode.hasAccess(flag: Int) = (this.access and flag) != 0

@PsiUtil.AccessLevel
val MethodNode.accessLevel
    get() = accessLevelFromFlags(this.access)

fun MethodNode.hasModifier(@PsiModifier.ModifierConstant modifier: String) = hasModifier(this.access, modifier)

val MethodNode.memberReference
    get() = MemberReference(this.name, this.desc)

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

fun MethodNode.findStubMethod(clazz: ClassNode, project: Project): PsiMethod? {
    return clazz.findStubClass(project)?.findMethods(memberReference)?.firstOrNull()
}

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

    val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
    val methodText = buildString {
        append("public ")
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
            append(returnType.className)
            append(' ')
            append(this@findOrConstructSourceMethod.name.toJavaIdentifier())
        }
        append('(')
        val params = Type.getArgumentTypes(this@findOrConstructSourceMethod.desc)
        for ((index, param) in params.withIndex()) {
            if (index != 0) {
                append(", ")
            }
            var typeName = param.className
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
    val psiMethod = elementFactory.createMethodFromText(methodText, null)
    psiMethod.realName = name

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
    modifierList.setModifierProperty(PsiModifier.PUBLIC, hasAccess(Opcodes.ACC_PUBLIC))
    modifierList.setModifierProperty(PsiModifier.PROTECTED, hasAccess(Opcodes.ACC_PROTECTED))
    modifierList.setModifierProperty(PsiModifier.PRIVATE, hasAccess(Opcodes.ACC_PRIVATE))
    modifierList.setModifierProperty(PsiModifier.STATIC, hasAccess(Opcodes.ACC_STATIC))
    modifierList.setModifierProperty(PsiModifier.FINAL, hasAccess(Opcodes.ACC_FINAL))
    modifierList.setModifierProperty(PsiModifier.SYNCHRONIZED, hasAccess(Opcodes.ACC_SYNCHRONIZED))
    modifierList.setModifierProperty(PsiModifier.NATIVE, hasAccess(Opcodes.ACC_NATIVE))

    // put the method inside the class, if given
    return clazz
        ?.constructClass(project, "void foo(){}")
        ?.findMethodsByName("foo", false)
        ?.firstOrNull()
        ?.replace(psiMethod) as? PsiMethod
        ?: psiMethod
}

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
