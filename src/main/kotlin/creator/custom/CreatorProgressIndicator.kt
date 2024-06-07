package com.demonwav.mcdev.creator.custom

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.util.ProgressIndicatorBase

class CreatorProgressIndicator(
    val loadingProperty: GraphProperty<Boolean>? = null,
    val textProperty: GraphProperty<String>? = null,
    val text2Property: GraphProperty<String>? = null,
) : ProgressIndicatorBase(false, false) {

    init {
        loadingProperty?.set(false)
        textProperty?.set("")
        text2Property?.set("")
    }

    override fun start() {
        super.start()
        loadingProperty?.set(true)
    }

    override fun finish(task: TaskInfo) {
        super.finish(task)
        loadingProperty?.set(false)
    }

    override fun setText(text: String?) {
        super.setText(text)
        textProperty?.set(text ?: "")
    }

    override fun setText2(text: String?) {
        super.setText2(text)
        text2Property?.set(text ?: "")
    }
}
