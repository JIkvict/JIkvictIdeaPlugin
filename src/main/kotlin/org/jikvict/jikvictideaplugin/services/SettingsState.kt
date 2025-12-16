package org.jikvict.jikvictideaplugin.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "org.jikvict.jikvictideaplugin.services.SettingsState",
    storages = [Storage("JikvictPluginSettings.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {
    var jwtToken: String = ""

    override fun getState(): SettingsState = this

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): SettingsState = service()
    }
}
