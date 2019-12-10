package com.example.demo.app.directory

import javafx.concurrent.Task
import tornadofx.*
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class FileManagerService {

    @Volatile
    private var directory = ""
    private var walkerTask: Task<Unit>? = null
    private val mutex = AtomicBoolean(true)

    private val fileTree = ConcurrentSkipListSet<ComparableFile>()
    private val fileMap = ConcurrentHashMap<String, ComparableFile>()

    fun getFiles(): Map<String, ComparableFile> {
        val result = HashMap<String, ComparableFile>()
        runWithLock { result.putAll(fileTree.map { Pair(it.path, it) }) }
        return result
    }

    fun startProcess(path: String) {
        directory = path
        if (walkerTask != null && walkerTask?.cancel() == false) {
            throw RuntimeException("can't close previous task")
        }
        walkerTask = startWalkOnDirectory()
    }

    fun deleteFile(file: ComparableFile) {
        runWithLock {
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
                if (path != directory) {
                    break
                }
                runWithLock {
                    pathsToDelete.forEach {
                        val file = fileMap[it]
                        fileTree.remove(file)
                        fileMap.remove(it)
                    }
                }
            }
        }
    }

    private fun clearFileMapAndFileTree() {
        runWithLock {
            fileMap.clear()
            fileTree.clear()
        }
    }

    private fun runWithLock(f: () -> Unit) {
        while (!mutex.compareAndSet(true, false)) {  }
        try {
            f.invoke()
        } finally {
            mutex.set(true)
        }
    }

    private fun addFileToMapAndTree(file: File) {
        val newRecord = ComparableFile(file, getFileLength(file))
        val lastRecord = fileMap[newRecord.path]
        if (lastRecord == null || lastRecord.size != newRecord.size) {
            runWithLock {
                fileMap[newRecord.path] = newRecord
                fileTree.add(newRecord)
            }
        }
    }

    private fun getFileLength(file: File): Long {
        return if (file.isDirectory) {
            if (file.listFiles() != null) file.listFiles().map { getFileLength(it) }.sum()
            else 0L
        }
        else
            file.length()
    }
}