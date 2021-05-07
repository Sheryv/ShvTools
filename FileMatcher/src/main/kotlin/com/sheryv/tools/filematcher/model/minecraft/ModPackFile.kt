package com.sheryv.tools.filematcher.model.minecraft

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModPackFile(
    val projectId: Int,
    val downloadUrl: String,
    val fileDate: OffsetDateTime,
    val fileName: String,
    val id: Int,
    val FileNameOnDisk: String? = null,
//    val alternateFileId: Int? = null,
//    val categorySectionPackageType: Int? = null,
//    val dependencies: List<Any>? = null,
    val displayName: String? = null,
    val fileLength: Long? = null,
    val fileStatus: Int? = null,
//    val gameId: Int? = null,
    val gameVersion: List<String>? = null,
//    val gameVersionDateReleased: String? = null,
//    val hasInstallScript: Boolean? = null,
//    val isAlternate: Boolean? = null,
    val isAvailable: Boolean? = null,
//    val isCompatibleWithClient: Boolean? = null,
//    val isServerPack: Boolean? = null,
//    val packageFingerprint: Int? = null,
//    val releaseType: Int? = null,
//    val restrictProjectFileAccess: Int? = null,
//    val serverPackFileId: Int? = null,
    val packageFingerprint: String? = null,
    val projectStatus: Int? = null
)
