package com.sheryv.tools.filematcher.service

import com.fasterxml.jackson.databind.util.StdDateFormat
import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.*
import com.sheryv.util.Strings
import javafx.stage.Window
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

class RepositoryService {
  
  fun loadRepositoryConfig(url: String): Repository {
    val repo = BundleUtils.nginxExample()
    val result = Validator().validateRepo(repo)
    if (!result.isOk()) {
      throw ValidationError(result)
    }
    return repo
  }
  
  fun loadRepositoryFromFile(owner: Window): Repository? {
    return DialogUtils.openFileDialog(owner, initialFile = Configuration.get().lastLoadedRepoFile).map {
      Configuration.get().lastLoadedRepoFile = it.toAbsolutePath().toString()
      Configuration.get().save()
      return@map loadRepositoryFromFile(it)
    }.orElse(null)
  }
  
  fun loadRepositoryFromFile(file: Path): Repository? {
    val mapper = if (file.toFile().extension == "json") DataUtils.jsonMapper() else DataUtils.yamlMapper()
    val repo = mapper.readValue(file.toFile(), Repository::class.java)
    
    val result = Validator().validateRepo(repo)
    if (!result.isOk()) {
      throw ValidationError(result)
    }
    return repo
  }
  
  
  fun generateFromLocalDir(dir: File): Repository {
    val file = if (!dir.isDirectory) {
      dir.parentFile
    } else {
      dir
    }
    val entries = loadEntriesFromFiles(file!!, file)
    
    val bundles = listOf(Bundle("bundle1", "Example bundle", versions = listOf(BundleVersion(
        versionId = 1,
        versionName = "0.1.0",
        entries = entries
    ))))
    
    return Repository("http://localhost/", "codeName", "1.0", 1,
        "http://localhost/",
        "Example title", "", mapOf("param1" to 123.toString()), bundles = bundles)
  }
  
  fun saveToFile(repo: Repository, file: File, format: String) {
    val mapper = if (format.toUpperCase() == "JSON") {
      DataUtils.jsonMapper()
    } else {
      DataUtils.yamlMapper()
    }
    
    mapper.writeValue(file, repo)
  }
  
  private fun loadEntriesFromFiles(dir: File, rootDir: File, parent: Entry? = null): List<Entry> {
    val list = mutableListOf<Entry>()
    for (child in dir.listFiles()!!) {
      if (child.isDirectory) {
        
        if (child.listFiles()?.size ?: 0 > 0) {
          val entry = createEntry(child, rootDir, parent)
          list.add(entry)
          list.addAll(loadEntriesFromFiles(child, rootDir, entry))
        }
        
      } else if (child.isFile) {
        list.add(createEntry(child, rootDir, parent))
      }
    }
    return list
  }
  
  private fun createEntry(file: File, rootDir: File, parent: Entry?): Entry {
    val path = file.toRelativeString(rootDir).replace('\\', '/')
    val map = mapOf("generatedAt" to Utils.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    return if (file.isDirectory) {
      BundleUtils.createGroup(Strings.generateId(4), file.name, parent = parent?.id, itemDate = Utils.now(), additionalFields = map)
    } else {
      val md5 = Hashing.md5(file.toPath())
      Entry(Strings.generateId(4), file.name, path, null, parent = parent?.id, itemDate = Utils.now(), hashes = Hash(md5), additionalFields = map)
    }
  }
}