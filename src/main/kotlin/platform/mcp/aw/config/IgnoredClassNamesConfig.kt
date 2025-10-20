package com.demonwav.mcdev.platform.mcp.aw.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "IgnoredClassNamesConfig", storages = [Storage("ignoredClassNames.xml")])
class IgnoredClassNamesConfig : PersistentStateComponent<IgnoredClassNamesConfig.State> {

    data class State(var ignoredClassNames: MutableSet<String> = mutableSetOf())

    private var state = State()

    companion object {
        fun getInstance(project: Project): IgnoredClassNamesConfig =
            project.getService(IgnoredClassNamesConfig::class.java)
    }

    var ignoredClassNames: MutableSet<String>
        get() = state.ignoredClassNames
        set(value) {
            state.ignoredClassNames = value
        }

    override fun getState(): State = state
    override fun loadState(state: State) {
        this.state = state
    }
}
