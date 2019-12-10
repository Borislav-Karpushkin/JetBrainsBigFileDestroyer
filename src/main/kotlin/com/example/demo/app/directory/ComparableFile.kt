package com.example.demo.app.directory

import java.io.File

class ComparableFile(
        val name: String,
        val path: String,
        var size: Long,
        val isDirectory: Boolean
): Comparable<ComparableFile> {

    companion object {
        val KB_SIZE = 1024;
        val MB_SIZE = KB_SIZE * 1024;
        val GB_SIZE = MB_SIZE * 1024
    }

    val directory = if (this.isDirectory) "directory" else "file"
    var sizeAsString = when {
        this.size > GB_SIZE -> (this.size.toDouble() / GB_SIZE).toString().take(5) + " Gb"
        this.size > MB_SIZE -> (this.size.toDouble() / MB_SIZE).toString().take(5) + " Mb"
        this.size > KB_SIZE -> (this.size.toDouble() / KB_SIZE).toString().take(5) + " Kb"
        else -> "$size b"
    }

    constructor(file: File) : this(name = file.name, path = file.absolutePath, size = file.length(), isDirectory = file.isDirectory)
    constructor(file: File, length: Long) : this(name = file.name, path = file.absolutePath, size = length, isDirectory = file.isDirectory)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComparableFile

        if (name != other.name) return false
        if (path != other.path) return false
        if (isDirectory != other.isDirectory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + isDirectory.hashCode()
        return result
    }

    override fun compareTo(other: ComparableFile): Int {
        return if (this.size == other.size)
            this.path.compareTo(other.path)
        else
            this.size.compareTo(other.size) * -1
    }

    override fun toString(): String {
        return "ComparableFile(name='$name', path='$path', size=$size, isDirectory=$isDirectory)"
    }


}