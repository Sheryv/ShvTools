package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.minecraft.Addon
import com.sheryv.tools.filematcher.model.minecraft.InstanceConfig
import com.sheryv.tools.filematcher.utils.DataUtils
import java.io.File
import java.nio.file.Paths

class MinecraftService {
  fun fillDataFromCurseForgeJson(repo: Repository? = null, curseForgeSource: File, output: String): Repository {
    val repository = repo ?: RepositoryService().loadRepositoryFromFile(Paths.get(output))
        ?: throw IllegalStateException("Cannot load repository from: $output")
    
    val instance = DataUtils.jsonMapper().readValue(curseForgeSource, InstanceConfig::class.java)
    repository.bundles.forEach { b ->
      b.versions.forEach { v ->
        v.entries = v.entries.map { e ->
          findAddon(e.name, instance)?.let { a ->
            val map = e.additionalFields + mapOf(
                "projectId" to a.addonID.toString(),
                "fileId" to a.installedFile.id.toString(),
                "fileFingerPrint" to a.installedFile.packageFingerprint,
                "fileLength" to a.installedFile.fileLength.toString()
            )
            e.copy(src = a.installedFile.downloadUrl, itemDate = a.installedFile.fileDate, additionalFields = map, fileSize = a.installedFile.fileLength)
          } ?: e
        }
      }
    }
    return repository
  }
  
  private fun findAddon(name: String, instanceConfig: InstanceConfig): Addon? {
    return instanceConfig.installedAddons
        .firstOrNull {
          (it.installedFile.FileNameOnDisk ?: it.installedFile.fileName).toLowerCase() == name.toLowerCase()
        }
  }
}
