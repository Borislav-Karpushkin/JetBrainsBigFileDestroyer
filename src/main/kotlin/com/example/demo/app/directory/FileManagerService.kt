package com.example.demo.app.directory

import javafx.concurrent.Task
import tornadofx.*
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class FileManagerService {

    @Volatile
    private var directory = ""
    private var walkerTask: Task<Unit>? = null
    private val mutex = Mutex(true)

    private val fileTree = ConcurrentSkipListSet<ComparableFile>()
    private val fileMap = ConcurrentHashMap<String, ComparableFile>()

    fun getFiles(): Map<String, ComparableFile> {
        val result = HashMap<String, ComparableFile>()
        mutex.runWithLock {
            result.putAll(
                    fileTree.map {
                        Pair(it.path, it)
                    }
            )
        }
        return result
    }

    fun startProcess(path: String) {
        directory = path
        if (walkerTask != null && walkerTask?.cancel() == false) {
            throw RuntimeException("can't stop previous thread")
        }
        walkerTask = startWalkOnDirectory()
    }

    fun deleteFile(file: ComparableFile) {
        mutex.runWithLock {
            File(file.path).deleteRecursively()
            fileMap.remove(file.path)
            fileTree.remove(file)
        }
    }

    fun moveFile(file: ComparableFile, newPath: String) {
        if (file.isDirectory) {
            File(file.path).copyTo(File(newPath))
        } else {
            File(file.path).copyRecursively(File(newPath))
        }
        deleteFile(file)
    }

    private fun startWalkOnDirectory(): Task<Unit> {
        return runAsync {
            val path = directory
            clearFileMapAndFileTree()
            while (path == directory) {
                val pathsToDelete = HashSet(fileMap.keys)
                File(path).walkTopDown().forEach {
                    if (path == directory && directory != it.absolutePath) {
                        addFileToMapAndTree(it)
                        pathsToDelete.remove(it.absolutePath)
                    }
                }
                if (path == directory) {
                    removeFilesFromMapAndTreeThatWereDeletedInFileSystem(pathsToDelete)
                }
            }
        }
    }

    private fun removeFilesFromMapAndTreeThatWereDeletedInFileSystem(pathsToRemove: Collection<String>) {
        mutex.runWithLock {
            pathsToRemove.forEach {
                val file = fileMap[it]
                fileTree.remove(file)
                fileMap.remove(it)
            }
        }
    }

    private fun clearFileMapAndFileTree() {
        mutex.runWithLock {
            fileMap.clear()
            fileTree.clear()
        }
    }

    private fun addFileToMapAndTree(file: File) {
        val newRecord = ComparableFile(file, getFileLength(file))
        val lastRecord = fileMap[newRecord.path]
        if (lastRecord == null || lastRecord.length != newRecord.length) {
            mutex.runWithLock {
                fileMap[newRecord.path] = newRecord
                fileTree.add(newRecord)
            }
        }
    }

    private fun getFileLength(file: File): Long {
        return if (file.isDirectory && file.listFiles() != null)
            file.listFiles().map { getFileLength(it) }.sum()
        else
            file.length()
    }
}