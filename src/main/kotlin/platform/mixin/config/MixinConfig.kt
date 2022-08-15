/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config

import com.intellij.json.JsonElementTypes
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.lang.LanguageImportStatements
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager

class MixinConfig(private val project: Project, private var json: JsonObject) {

    private val generator = JsonElementGenerator(project)
    var autoReformat = true

    val isWritable: Boolean
        get() = json.isWritable

    val file: VirtualFile?
        get() = json.containingFile.virtualFile

    var pkg: String?
        get() = (json.findProperty("package")?.value as? JsonStringLiteral)?.value
        set(value) {
            if (value == null) {
                val prop = json.findProperty("package") ?: return
                deleteCommaAround(prop)
                prop.delete()
            } else {
                val prop = json.findProperty("package")
                if (prop == null) {
                    val packageValue = generator.createStringLiteral(value).text
                    val packageProperty = generator.createProperty("package", packageValue)
                    JsonPsiUtil.addProperty(json, packageProperty, false)
                } else {
                    prop.value?.replace(generator.createStringLiteral(value))
                }
            }
            if (autoReformat) {
                reformat()
            }
        }

    var mixins: MutableList<String?>
        get() = MixinList("mixins")
        set(value) {
            val prevAutoReformat = autoReformat
            autoReformat = false
            val v = mixins
            v.clear()
            v.addAll(value)
            autoReformat = prevAutoReformat
            if (autoReformat) {
                reformat()
            }
        }

    var client: MutableList<String?>
        get() = MixinList("client")
        set(value) {
            val prevAutoReformat = autoReformat
            autoReformat = false
            val v = client
            v.clear()
            v.addAll(value)
            autoReformat = prevAutoReformat
            if (autoReformat) {
                reformat()
            }
        }

    var server: MutableList<String?>
        get() = MixinList("server")
        set(value) {
            val prevAutoReformat = autoReformat
            autoReformat = false
            val v = server
            v.clear()
            v.addAll(value)
            autoReformat = prevAutoReformat
            if (autoReformat) {
                reformat()
            }
        }

    val qualifiedMixins: MutableList<String?>
        get() = FullyQualifiedMixinList(MixinList("mixins"))
    val qualifiedClient: MutableList<String?>
        get() = FullyQualifiedMixinList(MixinList("client"))
    val qualifiedServer: MutableList<String?>
        get() = FullyQualifiedMixinList(MixinList("server"))

    private fun deleteCommaAround(element: PsiElement) {
        // Delete the comma before, if it exists
        var elem = element.prevSibling
        while (elem != null) {
            if (elem.node.elementType === JsonElementTypes.COMMA || (elem is PsiErrorElement && elem.text == ",")) {
                elem.delete()
                return
            } else if (elem is PsiComment || elem is PsiWhiteSpace) {
                elem = elem.prevSibling
            } else {
                break
            }
        }

        // If it didn't exist, delete the comma after if it exists
        elem = element.nextSibling
        while (elem != null) {
            if (elem.node.elementType === JsonElementTypes.COMMA || (elem is PsiErrorElement && elem.text == ",")) {
                elem.delete()
                return
            } else if (elem is PsiComment || elem is PsiWhiteSpace) {
                elem = elem.nextSibling
            } else {
                break
            }
        }
    }

    private fun reformat() {
        json = CodeStyleManager.getInstance(project).reformat(json) as JsonObject
        file?.let { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return
            LanguageImportStatements.INSTANCE.forFile(psiFile).forEach { it.processFile(psiFile).run() }
            json = (PsiManager.getInstance(project).findFile(file) as JsonFile).topLevelValue as JsonObject
        }
    }

    private inner class FullyQualifiedMixinList(private val delegate: MixinList) : AbstractMutableList<String?>() {
        override val size: Int
            get() = delegate.size

        override fun add(index: Int, element: String?) = delegate.add(index, removePackage(element))

        override fun get(index: Int) = prependPackage(delegate[index])

        override fun removeAt(index: Int) = prependPackage(delegate.removeAt(index))

        override fun set(index: Int, element: String?) = prependPackage(delegate.set(index, removePackage(element)))

        private fun removePackage(element: String?): String? {
            val p = pkg ?: return null
            if (element?.startsWith("$p.") == false) return null
            return element?.substring(p.length + 1)
        }

        private fun prependPackage(result: String?) = result?.let { "$pkg.$result" }
    }

    private inner class MixinList(private val key: String) : AbstractMutableList<String?>() {

        private val array: List<JsonValue>?
            get() = (json.findProperty(key)?.value as? JsonArray)?.valueList

        override val size: Int
            get() = array?.size ?: 0

        override fun add(index: Int, element: String?) {
            val oldSize = size
            if (index < 0 || index > oldSize) {
                throw IndexOutOfBoundsException(index.toString())
            }
            val arr = getOrCreateJsonArray()
            val newValue = if (element == null) {
                generator.createValue("null")
            } else {
                generator.createStringLiteral(element)
            }
            when {
                oldSize == 0 -> {
                    // No comma added
                    arr.addAfter(newValue, arr.firstChild)
                }
                index == oldSize -> {
                    // Add comma before
                    val anchor = arr.lastChild
                    arr.addBefore(generator.createComma(), anchor)
                    arr.addBefore(newValue, anchor)
                }
                else -> {
                    // Add comma after
                    val anchor = arr.valueList[index]
                    arr.addBefore(newValue, anchor)
                    arr.addBefore(generator.createComma(), anchor)
                }
            }
            if (autoReformat) {
                reformat()
            }
        }

        override fun get(index: Int): String? {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException(index.toString())
            }
            return (array?.get(index) as? JsonStringLiteral)?.value
        }

        override fun removeAt(index: Int): String? {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException(index.toString())
            }
            val toDelete = array?.get(index) ?: return null
            val oldStr = (toDelete as? JsonStringLiteral)?.value
            deleteCommaAround(toDelete)
            toDelete.delete()
            if (autoReformat) {
                reformat()
            }
            return oldStr
        }

        override fun set(index: Int, element: String?): String? {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException(index.toString())
            }
            val toReplace = array?.get(index) ?: return null
            val oldStr = (toReplace as? JsonStringLiteral)?.value
            val newValue = if (element == null) {
                generator.createValue("null")
            } else {
                generator.createStringLiteral(element)
            }
            toReplace.replace(newValue)
            if (autoReformat) {
                reformat()
            }
            return oldStr
        }

        private fun getOrCreateJsonArray(): JsonArray {
            val existingArray = json.findProperty(key)?.value as? JsonArray
            if (existingArray != null) {
                return existingArray
            }
            val added = JsonPsiUtil.addProperty(json, generator.createProperty(key, "[]"), false)
            return (added as JsonProperty).value as JsonArray
        }
    }
}
