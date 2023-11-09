package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.BundleVersion
import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.Matching
import com.sheryv.tools.filematcher.model.minecraft.Addon
import com.sheryv.tools.filematcher.model.minecraft.InstanceConfig
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.util.logging.log
import java.io.File

private val URL_REGEX = Regex("curseforge\\.com/minecraft.+download/(\\d{5,})")
private val MOD_FULL_REGEX = Regex("([+A-Za-z\\d_-]+(\\d+)?)[_-]([\\w.]+[_-])?(.*)\\.jar")
private val MOD_SHORT_REGEX = Regex("([+A-Za-z\\d_-]+)([_-]\\w*\\d+)?\\.jar")
private val CHECK_MOD_FULL_REGEX = Regex("\\d\\.\\d")

class MinecraftService {
  
  fun fillDataFromCurseForgeJson(version: BundleVersion, curseForgeSource: File, output: String): BundleVersion {
//    val repository = repo ?: RepositoryService().loadRepositoryFromFile(Paths.get(output))
//    ?: throw IllegalStateException("Cannot load repository from: $output")
    
    val instance = DataUtils.jsonMapper().readValue(curseForgeSource, InstanceConfig::class.java)
//    repository.bundles.forEach { b ->
//      b.versions.forEach { v ->
    version.entries = version.entries.map { e ->
      findAddon(e.name, instance)?.let { a ->
        val map = e.additionalFields + mapOf(
          "projectId" to a.addonID.toString(),
          "fileId" to a.installedFile.id.toString(),
          "fileFingerPrint" to a.installedFile.packageFingerprint,
          "fileLength" to a.installedFile.fileLength.toString()
        )
        e.copy(
          src = a.installedFile.downloadUrl,
          itemDate = a.installedFile.fileDate,
          additionalFields = map,
          fileSize = a.installedFile.fileLength
        )
      } ?: e
//        }
//      }
    }
    return version
  }
  
  private fun findAddon(name: String, instanceConfig: InstanceConfig): Addon? {
    return instanceConfig.installedAddons
      .firstOrNull {
        (it.installedFile.FileNameOnDisk ?: it.installedFile.fileName).toLowerCase() == name.toLowerCase()
      }
  }
  
  public fun transformUrls(version: BundleVersion) {
    version.entries.forEach { e ->
      e.src = transformUrl(e.src, e.name)
    }
  }
  
  public fun addMinecraftModsMatcher(version: BundleVersion): List<String> {
    val errors = mutableListOf<String>()
    version.entries = version.entries.map {
      val (e, res) = addMinecraftModsMatcher(it)
      if (!res) {
        errors.add(e.name)
      }
      e
    }
    return errors
  }
  
  public fun updateUrlsFromText(text: String, version: BundleVersion) {
    text.lines().map { it.trim() }.forEach { url ->
      for (entry in version.entries) {
        if (url.contains(SystemUtils.encodeNameForWeb(entry.name))) {
          entry.src = url
        }
      }
    }
  }
  
  
  private fun addMinecraftModsMatcher(e: Entry): Pair<Entry, Boolean> {
    if (!e.name.endsWith(".jar")) {
      //ignore
      return Pair(e, true)
    }
    
    val matchResult = if (CHECK_MOD_FULL_REGEX.find(e.name) != null) {
      MOD_FULL_REGEX.matchEntire(e.name)
    } else {
      MOD_SHORT_REGEX.matchEntire(e.name)
    }
    
    if (matchResult != null) {
      val prefix = matchResult.groupValues[1].trim()
      val vGroup = if (matchResult.groupValues.size > 4) {
        4
      } else {
        2
      }
      val version = matchResult.groupValues[vGroup].trim()
      log.info("${prefix.padEnd(30)} | ${version.padEnd(20)} > ${e.name}")
      
      val targetPath = e.target.copy(matching = Matching(wildcard = "$prefix*.jar"))
      return Pair(e.copy(target = targetPath, version = if (version.isNotBlank()) version else e.version), true)
    }
    return Pair(e, false)
  }
  
  private fun transformUrl(url: String, fileName: String): String {
    val t = "https://edge.forgecdn.net/files/"
    val find = URL_REGEX.find(url)
    if (find != null) {
      val part = find.groupValues[1]
      return t + part.take(4) + "/" + part.substring(4) + "/" + fileName
    }
    return url;
  }
}
//https://mediafilez.forgecdn.net/files/3871/353/MouseTweaks-forge-mc1.19-2.23.jar
