package dev.sulan.argumentedcompletion.helper

import com.intellij.openapi.project.Project
import dev.sulan.ecproperty.settings.PropertySettingsService

/**
 * We don't want to re-calculate our trie everytime @see[dev.sulan.argumentedcompletion.headless.SimplePropertyCompletionContributor]
 * asks for it, that's why we cache it and only re-calculate it, if the user make any changes to the uploaded properties.
 *
 * @author Sulan Abubakarov
 */
object PropertyCache {
    private val projectTries = mutableMapOf<Project, Pair<Int, PropertyTrie>>()

    fun getTrie(project: Project): PropertyTrie {
        val state = PropertySettingsService.getInstance(project).state
        val currentHash = state.configs.hashCode()

        val cached = projectTries[project]
        if (cached == null || cached.first != currentHash) {
            val newTrie = PropertyTrie()
            state.configs.flatMap { it.properties }.forEach { newTrie.insert(it) }
            projectTries[project] = Pair(currentHash, newTrie)
        }
        return projectTries[project]!!.second
    }
}