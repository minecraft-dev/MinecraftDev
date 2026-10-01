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
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Quantifier
import com.demonwav.mcdev.util.mapToArray
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode

fun ClassNode.findMethods(refs: List<MixinSelector>, allowStatic: Boolean): Sequence<MethodNode> =
    TargetSelectors(this, refs, allowStatic).findMethods()

fun ClassNode.findReferences(refs: List<MixinSelector>, allowStatic: Boolean): Sequence<MemberReference> =
    TargetSelectors(this, refs, allowStatic).findReferences()

private class TargetSelectors(
    private val classNode: ClassNode,
    private val selectors: Collection<MixinSelector>,
    private val allowStatic: Boolean,
) {
    private val methodLookup: Map<Pair<String, String>, MethodNode> =
        classNode.methods.associateBy { it.name to it.desc }

    private val containedLambdas: Map<MethodNode, List<ElementNode>> by lazy {
        classNode.methods.associateWith { method ->
            findContainedLambdas(classNode, method) { element ->
                methodLookup.getValue(element.implName to element.implDesc)
            }
        }
    }

    private val allLambdas: Set<MethodNode> by lazy {
        containedLambdas.values.asSequence()
            .flatten()
            .mapTo(hashSetOf()) { findMethod(it) }
    }

    fun findMethods(): Sequence<MethodNode> = findElements().map { findMethod(it) }

    fun findReferences(): Sequence<MemberReference> = findElements().map {
        MemberReference(it.name, it.desc, it.owner.replace('/', '.'))
    }

    private fun findElements(): Sequence<ElementNode> = sequence {
        for (selector in selectors) {
            val roots = findRootTargets(selector)
            if (selector.next == null) {
                yieldAll(roots.map { ElementNodeMethod(classNode, it) })
                continue
            }

            var working = roots
                .filter { it !in allLambdas }
                .map<_, ElementNode> { ElementNodeMethod(classNode, it) }

            var currentSelector = selector
            while (true) {
                val minDepth = selector.nextDepth.min(Quantifier.Context.LAMBDA_DEPTH)
                val maxDepth = selector.nextDepth.max(Quantifier.Context.LAMBDA_DEPTH)
                currentSelector = currentSelector.next ?: break

                working = findNested(currentSelector, working, minDepth, maxDepth)
            }

            yieldAll(working)
        }
    }

    private fun findRootTargets(selector: MixinSelector): Sequence<MethodNode> {
        val maxMatches = selector.quantifier.max(Quantifier.Context.MEMBER)
        return classNode.methods.asSequence()
            .filter {
                selector.matchMethod(it, classNode)
                    && (maxMatches <= 1 || allowStatic || !it.hasAccess(Opcodes.ACC_STATIC))
            }.take(maxMatches)
    }

    private fun findNested(selector: MixinSelector, parents: Sequence<ElementNode>, minDepth: Int, maxDepth: Int) =
        sequence {
            for (parent in parents) {
                val stack = mutableListOf(parent withDepth 0)

                while (stack.isNotEmpty()) {
                    val (current, depth) = stack.removeLast()

                    if (depth >= minDepth && selector.matchMethod(current.owner, current.name, current.desc)) {
                        yield(current)
                    }

                    if (depth >= maxDepth) {
                        // Stop looking
                        continue
                    }

                    for (lambda in containedLambdas.getValue(findMethod(current)).asReversed()) {
                        stack.add(lambda withDepth depth + 1)
                    }
                }
            }
        }
            .distinct()
            .take(selector.quantifier.max(Quantifier.Context.LAMBDA))

    private fun findMethod(elementNode: ElementNode) =
        methodLookup.getValue(elementNode.implName to elementNode.implDesc)

    private companion object {
        private fun findContainedLambdas(
            owner: ClassNode,
            method: MethodNode,
            methodLookup: (ElementNode) -> MethodNode,
        ): List<ElementNode> =
            method.instructions.toArray().asSequence()
                .filterIsInstance<InvokeDynamicInsnNode>()
                .mapNotNull { ElementNodeLmfInsn.of(it) }
                .filter { candidate ->
                    candidate.implOwner == owner.name
                        && methodLookup(candidate).hasAccess(Opcodes.ACC_SYNTHETIC)
                        && !methodLookup(candidate).hasAccess(Opcodes.ACC_BRIDGE)
                }
                .toList()
    }
}

private data class WithDepth<out T>(val value: T, val depth: Int)

private infix fun <T> T.withDepth(depth: Int) = WithDepth(this, depth)

private sealed interface ElementNode {
    val owner: String
    val name: String
    val desc: String

    val implOwner: String
    val implName: String
    val implDesc: String
}

private data class ElementNodeMethod(private val classNode: ClassNode, private val methodNode: MethodNode) :
    ElementNode {
    override val owner: String get() = classNode.name
    override val name: String get() = methodNode.name
    override val desc: String get() = methodNode.desc

    override val implOwner get() = owner
    override val implName get() = name
    override val implDesc get() = desc
}

@ConsistentCopyVisibility
private data class ElementNodeLmfInsn private constructor(private val insn: InvokeDynamicInsnNode) : ElementNode {
    private val implMethod = insn.bsmArgs[1] as Handle
    private val instantiatedMethodType = insn.bsmArgs[2] as Type

    override val owner: String get() = Type.getReturnType(insn.desc).internalName
    override val name: String get() = insn.name
    override val desc: String get() = instantiatedMethodType.descriptor

    override val implOwner: String get() = implMethod.owner
    override val implName: String get() = implMethod.name
    override val implDesc: String get() = implMethod.desc

    companion object {
        private val LMF_HANDLE: Handle = Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            generateDescriptor(
                CallSite::class.java,
                MethodHandles.Lookup::class.java,
                String::class.java,
                MethodType::class.java,
                MethodType::class.java,
                MethodHandle::class.java,
                MethodType::class.java,
            ),
            false,
        )

        private val ALT_LMF_HANDLE: Handle = Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            generateDescriptor(
                CallSite::class.java,
                MethodHandles.Lookup::class.java,
                String::class.java,
                MethodType::class.java,
                Array<Any>::class.java,
            ),
            false,
        )

        fun of(insn: InvokeDynamicInsnNode): ElementNodeLmfInsn? {
            if (insn.bsm != LMF_HANDLE && insn.bsm != ALT_LMF_HANDLE) {
                return null
            }
            return ElementNodeLmfInsn(insn)
        }

        private fun generateDescriptor(returnClass: Class<*>, vararg parameterClasses: Class<*>): String {
            val returnType = Type.getType(returnClass)
            val parameterTypes = parameterClasses.mapToArray { Type.getType(it) }
            return Type.getMethodDescriptor(returnType, *parameterTypes)
        }
    }
}
