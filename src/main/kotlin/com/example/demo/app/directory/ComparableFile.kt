package com.example.demo.app.directory

import java.io.File

class ComparableFile(
        val name: String,
        val path: String,
        var length: Long,
        val isDirectory: Boolean
): Comparable<ComparableFile> {

    companion object {
        val KB_BYTES_SIZE = 1024;
        val MB_BYTES_SIZE = KB_BYTES_SIZE * 1024;
        val GB_BYTES_SIZE = MB_BYTES_SIZE * 1024
    }

    val directory = if (this.isDirectory) "directory" else "file"
    var sizeAsString = when {
        this.length > GB_BYTES_SIZE -> (this.length.toDouble() / GB_BYTES_SIZE).toString().take(5) + " Gb"
        this.length > MB_BYTES_SIZE -> (this.length.toDouble() / MB_BYTES_SIZE).toString().take(5) + " Mb"
        this.length > KB_BYTES_SIZE -> (this.length.toDouble() / KB_BYTES_SIZE).toString().take(5) + " Kb"
        else -> "$length b"
    }

    constructor(file: File) : this(
            name = file.name,
            path = file.absolutePath,
            length = file.length(),
            isDirectory = file.isDirectory
    )
    constructor(file: File, length: Long) : this(
            name = file.name,
            path = file.absolutePath,
            length = length,
            isDirectory = file.isDirectory
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComparableFile

        if (name != other.name) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun compareTo(other: ComparableFile): Int {
        return if (this.length == other.length)
            this.path.compareTo(other.path)
        else
            this.length.compareTo(other.length) * -1
    }

    override fun toString(): String {
        return "ComparableFile(name='$name', path='$path', length=$length, isDirectory=$isDirectory)"
    }


}