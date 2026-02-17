package dev.sulan.ecproperty.model

import com.intellij.util.xmlb.annotations.Tag
import kotlinx.serialization.Serializable

@Serializable
class Property {
    @Tag("name")
    var name: String = ""

    @Tag("description")
    var description: String? = null

    fun copy() = Property().apply {
        name = this@Property.name
        description = this@Property.description
    }

    fun hasDescription() = !description.isNullOrBlank()
}
