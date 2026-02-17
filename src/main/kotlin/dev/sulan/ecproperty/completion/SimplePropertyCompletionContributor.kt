package dev.sulan.ecproperty.completion

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import dev.sulan.ecproperty.model.FileTypeConfig
import dev.sulan.ecproperty.model.Property
import dev.sulan.ecproperty.settings.PropertySettingsService

/**
 * When ever the user types or selects an auto-complete option, the @see[CompletionProvider.addCompletions]
 * is called, so that we can provide auto-complete options, plus optionally a description.
 *
 * @author Sulan Abubakarov
 */
class SimplePropertyCompletionContributor : CompletionContributor() {

    private val CUSTOM_DESCRIPTION_PREFIX = "  [C]";

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    initialResult: CompletionResultSet
                ) {
                    val previousUserInput = getPreviousUserInput(parameters).trimStart()
                    if (!isValidPropertyDefinitionContext(previousUserInput)) return
                    var result = initialResult.withPrefixMatcher(PlainPrefixMatcher(previousUserInput))
                    val addedSuggestions = mutableSetOf<String>()

                    val matchingConfigs = getAllFileTypeConfigs(parameters) ?: return;
                    val uploadedProperties = matchingConfigs.flatMap { it.properties }.distinct()

                    uploadedProperties.forEach { prop ->
                        if (prop.name.startsWith(previousUserInput)) {
                            val remaining = prop.name.substring(previousUserInput.length)
                            val nextDot = remaining.indexOf('.')
                            val isPartial = nextDot != -1

                            val suggestion = if (isPartial) {
                                previousUserInput + remaining.substring(0, nextDot + 1)
                            } else {
                                prop.name
                            }

                            addSuggestion(addedSuggestions, suggestion, isPartial, prop, result)
                        }
                    }
                }
            }
        )
    }

    private fun addSuggestion(
        addedSuggestions: MutableSet<String>,
        suggestion: String,
        isPartial: Boolean,
        prop: Property,
        result: CompletionResultSet
    ) {
        if (addedSuggestions.add(suggestion)) {
            var builder = LookupElementBuilder.create(suggestion)
                .withPresentableText(suggestion)
                .withTailText(CUSTOM_DESCRIPTION_PREFIX, true)

            if (isPartial) {
                builder = triggerAnotherAutoCompleteOption(builder)
            } else {
                if (prop.hasDescription()) {
                    builder = fullAutoCompleteWithDescription(builder, prop)
                } else {
                    builder = fullAutoCompleteWithoutDescription(builder)
                }
            }

            result.addElement(PrioritizedLookupElement.withPriority(builder, Double.NEGATIVE_INFINITY))
        }
    }

    private fun triggerAnotherAutoCompleteOption(builder: LookupElementBuilder): LookupElementBuilder = builder.withInsertHandler { ctx, _ ->
        ctx.setLaterRunnable {
            CodeCompletionHandlerBase(CompletionType.BASIC)
                .invokeCompletion(ctx.project, ctx.editor)
        }
    }

    /**
     * In case the property is fully selected by the user, just insert a "=" at the end and auto complete it fully.
     */
    private fun fullAutoCompleteWithDescription(
        builder: LookupElementBuilder,
        prop: Property
    ): LookupElementBuilder {
        return builder.withTypeText(prop.description, true)
            .withInsertHandler { ctx, _ ->
                val editor = ctx.editor
                val offset = ctx.tailOffset
                editor.document.insertString(offset, "=")
                editor.caretModel.moveToOffset(offset + 1)
            }
    }

    /**
     * In case the property is fully selected by the user, just insert a "=" at the end and auto complete it fully.
     */
    private fun fullAutoCompleteWithoutDescription(
        builder: LookupElementBuilder,
    ): LookupElementBuilder {
        return builder.withInsertHandler { ctx, _ ->
                val editor = ctx.editor
                val offset = ctx.tailOffset
                editor.document.insertString(offset, "=")
                editor.caretModel.moveToOffset(offset + 1)
            }
    }

    private fun getAllFileTypeConfigs(parameters: CompletionParameters): List<FileTypeConfig>? {
        val project = parameters.editor.project ?: return null
        val state = PropertySettingsService.getInstance(project).state
        val fileName = parameters.originalFile.name
        val configurations = state.configs.filter { fileName.endsWith(".${it.extension}") }

        // In case the user doesn't have uploaded any properties for auto-completion.
        if (configurations.isEmpty()) return null
        return configurations
    }

    private fun getPreviousUserInput(parameters: CompletionParameters): @NlsSafe String {
        val caretOffset = parameters.offset
        val document = parameters.editor.document
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val fullLinePrefix = document.getText(TextRange(lineStartOffset, caretOffset))
        return fullLinePrefix
    }

    private fun isValidPropertyDefinitionContext(userTextInput: String): Boolean {
        return !userTextInput.contains("=") && !userTextInput.contains("#") && !userTextInput.contains("!")
    }
}