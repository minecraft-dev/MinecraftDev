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
import com.demonwav.mcdev.util.Quantifier
import com.intellij.openapi.util.text.StringUtil
import org.objectweb.asm.Type

/**
 * Represents a Mixin MemberInfo.
 */
data class MemberInfo(
    val name: String? = null,
    val descriptor: String? = null,
    override val owner: String? = null,
    override val quantifier: Quantifier = Quantifier.Default,
) : MixinSelector {

    init {
        assert(owner?.contains('/') != true)
    }

    val withoutOwner
        get() = if (this.owner == null) {
            this
        } else {
            copy(owner = null)
        }

    override val methodDescriptor = descriptor?.takeIf { it.contains("(") }
    override val fieldDescriptor = descriptor?.takeUnless { it.contains("(") }

    override fun canEverMatch(name: String): Boolean {
        return matchName(name)
    }

    private fun matchName(name: String): Boolean {
        return this.name == null || this.name == name
    }

    private fun matchOwner(clazz: String): Boolean {
        assert(!clazz.contains('.'))
        return this.owner == null || this.owner == clazz.replace('/', '.')
    }

    override fun matchField(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return matchName(name) &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
    }

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return matchName(name) &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
    }

    fun toMixinString(): String {
        return buildString {
            if (owner != null) {
                append('L').append(owner.replace('.', '/')).append(';')
            }

            name?.let(::append)
            append(quantifier)

            descriptor?.let { descriptor ->
                if (!descriptor.startsWith('(')) {
                    // Field descriptor
                    append(':')
                }

                append(descriptor)
            }
        }
    }

    override fun withQuantifier(quantifier: Quantifier) = copy(quantifier = quantifier)

    companion object {
        fun parse(input: String): MemberInfo? {
            var desc: String? = null
            var owner: String? = null
            var name: String = input.trim()

            val parenPos = name.indexOf('(')
            val colonPos = name.indexOf(':')
            if (parenPos > -1) {
                desc = name.substring(parenPos).trim()
                name = name.substring(0, parenPos).trim()
            } else if (colonPos > -1) {
                desc = name.substring(colonPos + 1).trim()
                name = name.substring(0, colonPos).trim()
            }

            val lastDotPos = name.lastIndexOf('.')
            val semiColonPos = name.indexOf(';')
            if (lastDotPos > -1) {
                owner = name.substring(0, lastDotPos).replace('/', '.').trim()
                name = name.substring(lastDotPos + 1).trim()
            } else if (semiColonPos > -1 && name.startsWith("L")) {
                owner = name.substring(1, semiColonPos).replace('/', '.').trim()
                name = name.substring(semiColonPos + 1).trim()
            }

            if ((name.contains('/') || name.contains('.')) && owner == null) {
                owner = name.replace('/', '.')
                name = ""
            }

            var quantifier: Quantifier = Quantifier.Default
            if (name.endsWith('*')) {
                quantifier = Quantifier.Any
                name = name.dropLast(1).trim()
            } else if (name.endsWith('+')) {
                quantifier = Quantifier.Plus
                name = name.dropLast(1).trim()
            } else if (name.endsWith('}')) {
                val bracePos = name.indexOf('{')
                if (bracePos >= 0) {
                    quantifier = Quantifier.parse(name.substring(bracePos, name.length)) ?: return null
                    name = name.substring(0, bracePos).trim()
                }
            } else if (name.contains('{')) {
                return null // Probably incomplete quantifier
            }

            if (owner != null && !StringUtil.isJavaIdentifier(owner.replace('.', '_'))) {
                return null
            }
            if (name.isNotEmpty() && !StringUtil.isJavaIdentifier(name) && name != "<init>" && name != "<clinit>") {
                return null
            }

            return MemberInfo(name.takeIf { it.isNotEmpty() }, desc, owner, quantifier)
        }
    }
}
