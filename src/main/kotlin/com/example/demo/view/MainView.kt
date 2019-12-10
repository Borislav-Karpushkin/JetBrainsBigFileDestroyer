package com.example.demo.view

import com.example.demo.app.Styles
import com.example.demo.app.directory.ComparableFile
import com.example.demo.app.directory.FileManagerService
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ContextMenu
import tornadofx.*
import kotlin.system.exitProcess

class MainView : View("Big File Destroyer") {

    private val model = ViewModel()
    private val directory = model.bind { SimpleStringProperty() }
    private val fileManagerService = FileManagerService()
    private val topFiles = FXCollections.observableArrayList<ComparableFile>()

    override val root = vbox {
        hbox {
            button {
                alignment = Pos.TOP_LEFT
                text = "Choose the directory to analyze"
                style = "-fx-base: #57b757;"
                setOnAction {
                    chooseDirectory("Choose directory to analyze")?.absolutePath?.also {
                        fileManagerService.startProcess(it)
                        directory.value = "Current directory: $it"
                    }
                }
            }
            label(directory) {
                addClass(Styles.heading)
            }
        }
        tableview<ComparableFile> {
            items = topFiles
            readonlyColumn("name", ComparableFile::name).weightedWidth(3)
            readonlyColumn("type", ComparableFile::directory).minWidth(80).maxWidth(80)
            readonlyColumn("path", ComparableFile::path).weightedWidth(4)
            readonlyColumn("size", ComparableFile::sizeAsString).minWidth(80).maxWidth(80)
            readonlyColumn("size (bytes)", ComparableFile::length).minWidth(80).maxWidth(100)
            fitToParentSize()
            contextMenu = ContextMenu().apply {
                item("Delete").action {
                    selectedItem?.apply {
                        warning(header = "Delete",
                                content = "Do you really want to delete file or folder?",
                                actionFn = { fileManagerService.deleteFile(selectedItem!!) })
                    }
                }
                item("Replace").action {
                    selectedItem?.apply {
                        chooseDirectory("Choose directory to move file")?.absolutePath?.also { newPath ->
                            fileManagerService.moveFile(selectedItem!!, newPath + "\\" + selectedItem!!.name)
                        }
                    }
                }
            }
            prefHeight = 600.0
            prefWidth = 800.0
            columnResizePolicy = SmartResize.POLICY
        }
    }

    override fun onBeforeShow() {
        updateFields()
        super.onBeforeShow()
    }

    private fun updateFields() {
        runAsync {
            while(true) {
                Thread.sleep(500)
                val files = fileManagerService.getFiles()
                topFiles.removeIf { !files.containsKey(it.path) }
                topFiles.forEach {
                    it.length = files[it.path]!!.length
                }
                files.forEach { (_, u) -> if (!topFiles.contains(u)) topFiles.add(u) }
                topFiles.sortByDescending { it.length }
            }
        }
    }

    override fun onUndock() {
        exitProcess(0)
    }
}