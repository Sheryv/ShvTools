package com.sheryv.tools.filematcher.model

data class TargetPath(
    val path: BasePath? = null,
    val absolute: Boolean = false,
    val override: Boolean = true
) {
}