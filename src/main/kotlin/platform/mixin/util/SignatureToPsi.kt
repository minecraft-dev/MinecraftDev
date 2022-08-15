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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameterList
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

class SignatureToPsi(
    private val elementFactory: PsiElementFactory,
    private val context: PsiElement?,
    private val typeCompletedCallback: ((PsiType) -> Unit)? = null
) : SignatureVisitor(Opcodes.ASM7) {
    private val text = StringBuilder()

    private var hadBound = false
    private var hadTypeArgument = false
    private var arrayDimensions = 0
    private var lastTypeArgumentVisitor: SignatureToPsi? = null

    val formalTypeParameters: PsiTypeParameterList?
        get() {
            if (text.isEmpty()) {
                return null
            }
            if (!text.endsWith('>')) {
                text.append('>')
            }
            return elementFactory.createClassFromText("class Foo$text {}", context).typeParameterList!!
        }

    val type: PsiType
        get() = elementFactory.createTypeFromText(text.toString(), context)

    var superclassType: PsiType? = null
    val interfaceTypes = mutableListOf<PsiType>()

    val parameterTypes = mutableListOf<PsiType>()
    var returnType: PsiType? = null
    val exceptionTypes = mutableListOf<PsiType>()

    override fun visitFormalTypeParameter(name: String) {
        if (text.isEmpty()) {
            text.append('<')
        } else {
            text.append(", ")
        }
        text.append(name)
        hadBound = false
    }

    override fun visitClassBound(): SignatureVisitor {
        text.append(" extends ")
        hadBound = true
        return this
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        if (hadBound) {
            text.append(" & ")
        } else {
            text.append(" extends ")
            hadBound = true
        }
        return this
    }

    override fun visitSuperclass(): SignatureVisitor {
        return SignatureToPsi(elementFactory, context) {
            superclassType = it
        }
    }

    override fun visitInterface(): SignatureVisitor {
        return SignatureToPsi(elementFactory, context) {
            interfaceTypes += it
        }
    }

    override fun visitParameterType(): SignatureVisitor {
        return SignatureToPsi(elementFactory, context) {
            parameterTypes += it
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        return SignatureToPsi(elementFactory, context) {
            returnType = it
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return SignatureToPsi(elementFactory, context) {
            exceptionTypes += it
        }
    }

    private fun onTypeCompleted() {
        if (arrayDimensions > 0) {
            text.append("[]".repeat(arrayDimensions))
            arrayDimensions = 0
        }
        typeCompletedCallback?.let { it(this.type) }
    }

    override fun visitBaseType(descriptor: Char) {
        text.append(Type.getType(descriptor.toString()).className)
        onTypeCompleted()
    }

    override fun visitTypeVariable(name: String) {
        text.append(name)
        onTypeCompleted()
    }

    override fun visitArrayType(): SignatureVisitor {
        arrayDimensions++
        return this
    }

    override fun visitClassType(name: String) {
        text.append(name.replace('/', '.').replace('$', '.'))
    }

    private fun appendLastTypeArgument() {
        val lastVisitor = lastTypeArgumentVisitor
        if (lastVisitor != null) {
            text.append(lastVisitor.text)
            lastTypeArgumentVisitor = null
        }
    }

    override fun visitTypeArgument() {
        appendLastTypeArgument()
        if (hadTypeArgument) {
            text.append(", ")
        } else {
            text.append('<')
            hadTypeArgument = true
        }
        text.append("?")
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        appendLastTypeArgument()
        if (hadTypeArgument) {
            text.append(", ")
        } else {
            text.append('<')
            hadTypeArgument = true
        }
        if (wildcard == EXTENDS) {
            text.append("? extends ")
        } else if (wildcard == SUPER) {
            text.append("? super ")
        }

        val visitor = SignatureToPsi(elementFactory, context)
        lastTypeArgumentVisitor = visitor
        return visitor
    }

    override fun visitInnerClassType(name: String) {
        appendLastTypeArgument()
        if (hadTypeArgument) {
            text.append('>')
            hadTypeArgument = false
        }
        text.append('.').append(name)
    }

    override fun visitEnd() {
        appendLastTypeArgument()
        if (hadTypeArgument) {
            text.append('>')
        }
        onTypeCompleted()
    }
}
