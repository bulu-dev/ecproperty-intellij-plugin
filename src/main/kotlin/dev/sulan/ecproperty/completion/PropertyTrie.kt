package dev.sulan.argumentedcompletion.helper

import dev.sulan.ecproperty.model.Property

/**
 * A more optimized way for auto-completion ( O(L) where L is the length of the user input ) rather
 * than using a list and trying to call "startsWith".
 *
 * @author Sulan Abubakarov
 */
class PropertyTrie {
    private class Node {
        val children = mutableMapOf<Char, Node>()
        var entry: Property? = null
    }

    private val root = Node()

    fun insert(entry: Property) {
        var current = root
        for (char in entry.name) {
            current = current.children.getOrPut(char) { Node() }
        }
        current.entry = entry
    }

    fun findByPrefix(prefix: String): List<Property> {
        var current = root
        for (char in prefix) {
            current = current.children[char] ?: return emptyList()
        }

        val results = mutableListOf<Property>()
        collectAll(current, results)
        return results
    }

    private fun collectAll(node: Node, results: MutableList<Property>) {
        node.entry?.let { results.add(it) }
        node.children.values.forEach { collectAll(it, results) }
    }
}
