package dev.sulan.ecproperty.settings

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import dev.sulan.ecproperty.model.FileTypeConfig

/**
 * This services is needed to persist the uploaded properties-JSON files and settings.
 *
 * @author Sulan Abubakarov
 */
@Service(Service.Level.PROJECT)
@State(name = "ECPropertySettings", storages = [Storage("ecproperty.xml")])
class PropertySettingsService : PersistentStateComponent<PropertySettingsService.State> {

    class State {
        @XCollection()
        @Tag("configs")
        var configs: MutableList<FileTypeConfig> = mutableListOf()
    }

    private var myState = State()

    override fun getState() = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(project: Project): PropertySettingsService = project.service()
    }
}
