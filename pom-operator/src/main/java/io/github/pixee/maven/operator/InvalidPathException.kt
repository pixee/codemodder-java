package io.github.pixee.maven.operator

import java.io.File
import java.io.IOException

class InvalidPathException(
    val parentPath: File,
    val relativePath: String,
    val loop: Boolean = false
) : IOException("Invalid Relative Path $relativePath (from ${parentPath.absolutePath}) (loops? ${loop})")