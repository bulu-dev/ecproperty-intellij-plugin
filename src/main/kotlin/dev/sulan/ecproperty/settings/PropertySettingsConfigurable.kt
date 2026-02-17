package dev.sulan.ecproperty.settings

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.sulan.ecproperty.model.FileTypeConfig
import dev.sulan.ecproperty.model.Property
import kotlinx.serialization.json.Json
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

/**
 * Provides a new option/ui under "settings", to upload properties for auto-completion.
 *
 * @author Sulan Abubakarov
 */
class PropertySettingsConfigurable(private val project: Project) : Configurable {
    private val settings = PropertySettingsService.getInstance(project)

    // Table definition with two columns
    private val tableModel = ListTableModel<FileTypeConfig>(
        object : ColumnInfo<FileTypeConfig, String>("File-Extension") {
            override fun valueOf(item: FileTypeConfig) = item.extension
            override fun setValue(item: FileTypeConfig, value: String) {
                item.extension = value
            }

            override fun isCellEditable(item: FileTypeConfig) = true
            override fun getTooltipText(): String = "File type for which to apply auto completion e. g. properties, java ..."
        },
        object : ColumnInfo<FileTypeConfig, String>("Properties") {
            override fun valueOf(item: FileTypeConfig) = "${item.properties.size} properties loaded"
            override fun getRenderer(item: FileTypeConfig) = DefaultTableCellRenderer().apply { text = valueOf(item) }
            override fun getTooltipText(): String = "A list of properties with the following JSON structure: [ { name, description } ]."
        }
    )

    // Preview list after properties were loaded
    private val previewListModel = DefaultListModel<String>()
    private val previewList = JBList(previewListModel)

    private val DOUBLE_CLICK = 2
    private val PANEL_HEIGHT = 600
    private val PANEL_WIDTH = 800
    private val ALLOWED_PROPERTIES_FILE_TYPE = "json"

    // Main logic
    override fun createComponent(): JComponent {
        val table = JBTable(tableModel)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        openDialogForPropertiesAfterDoubleClick(table)
        refreshPropertiesPreviewAfterRowSelection(table)

        return panel(defineToolbarActionsAndBehaviour(table))
    }

    private fun defineToolbarActionsAndBehaviour(table: JBTable): ToolbarDecorator {
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel.addRow(FileTypeConfig())
                importJson(table.rowCount - 1, table)
            }
            .setRemoveAction {
                tableModel.removeRow(table.selectedRow)
            }
            .addExtraAction(object : AnAction(
                "Import Properties",
                "Properties with the following JSON structure: [ { name, description } ].",
                AllIcons.Actions.Upload
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    val row = table.selectedRow
                    if (selectedRow(row)) importJson(row, table)
                }
            })
        return decorator
    }

    private fun panel(decorator: ToolbarDecorator): DialogPanel = panel {
        group("Properties configuration") {
            row {
                scrollCell(decorator.createPanel())
                    .align(Align.FILL)
            }.resizableRow()
        }

        group("Properties preview") {
            row {
                scrollCell(previewList)
                    .align(Align.FILL)
                    .applyToComponent {
                        emptyText.text = "Select table row, to preview properties of that selection."
                    }
            }.resizableRow()
        }
    }.apply {
        preferredSize = Dimension(PANEL_WIDTH, PANEL_HEIGHT)
    }

    private fun refreshPropertiesPreviewAfterRowSelection(table: JBTable) {
        table.selectionModel.addListSelectionListener {
            val row = table.selectedRow
            previewListModel.clear()

            if (selectedRow(row)) {
                val config = tableModel.getItem(row)
                config.properties.forEach { previewListModel.addElement("${it.name} — ${it.description}") }
            }
        }
    }

    private fun selectedRow(row: Int): Boolean = (row as? Int? ?: -1) != -1

    private fun openDialogForPropertiesAfterDoubleClick(table: JBTable) {
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == DOUBLE_CLICK) { // Nur bei Doppelklick
                    val row = table.rowAtPoint(e.point)
                    val col = table.columnAtPoint(e.point)
                    val propertiesColumn = 1

                    if (selectedRow(row) && col == propertiesColumn) {
                        importJson(row, table)
                    }
                }
            }
        })
    }

    private fun importJson(row: Int, table: JBTable) {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { it.extension == ALLOWED_PROPERTIES_FILE_TYPE }

        FileChooser.chooseFile(descriptor, project, null) { virtualFile ->
            try {
                val fileContent = String(virtualFile.contentsToByteArray())
                val importedProperties = parseToPropertyEntries(fileContent)

                val rowFileTypeConfig = tableModel.getItem(row)
                rowFileTypeConfig.properties = importedProperties.toMutableList()

                refreshTable()
                selectAutomaticallyTheCurrentRow(table, row)
            } catch (ex: Exception) {
                showErrorDialogIfFileContentIsMalformed(ex)
            }
        }

        if (tableModel.getItem(row).properties.isEmpty()) {
            removeLastTableRow(table)
        }
    }

    private fun removeLastTableRow(table: JBTable) {
        val lastAdded = table.rowCount - 1
        tableModel.removeRow(lastAdded)
    }

    private fun refreshTable() {
        tableModel.fireTableDataChanged()
    }

    private fun showErrorDialogIfFileContentIsMalformed(ex: Exception) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ECProperty Group")
            .createNotification(
                "PROPERTIES IMPORT ERROR",
                "File didn't have the right format ([ { name, description } ]): ${ex.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    /**
     * Is needed so that the preview will be shown, as soon as the properties are loaded @see[refreshPropertiesPreviewAfterRowSelection].
     */
    private fun selectAutomaticallyTheCurrentRow(table: JBTable, row: Int) {
        table.selectionModel.setSelectionInterval(row, row)
    }

    /**
     * Parse the properties-JSON into @see[Property].
     */
    private fun parseToPropertyEntries(content: String): List<Property> = Json.decodeFromString<List<Property>>(content)

    override fun isModified(): Boolean {
        return tableModel.items != settings.state.configs
    }

    override fun apply() {
        settings.state.configs = tableModel.items.map { it.copy() }.toMutableList()
    }

    override fun reset() {
        tableModel.items = settings.state.configs.map { it.copy() }.toMutableList()
        previewListModel.clear()
    }

    override fun getDisplayName() = "ECProperty Completion"
}