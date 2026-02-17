package dev.sulan.ecproperty.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import kotlinx.serialization.Serializable

@Serializable
class FileTypeConfig {
    @Tag("extension")
    var extension: String = "properties"

    @XCollection()
    @Tag("properties")
    var properties: MutableList<Property> = mutableListOf()

    // Hilfsmethode für eine echte Kopie (verhindert Referenz-Fehler)
    fun copy() = FileTypeConfig().apply {
        extension = this@FileTypeConfig.extension
        properties = this@FileTypeConfig.properties.map { it.copy() }.toMutableList()
    }
}