package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.lg
import javafx.stage.Window
import java.io.File
import java.nio.file.*

class RepositoryService {
  
  fun loadRepositoryConfig(url: String): Repository {
    val repo = DataUtils.downloadAndParse(url, Repository::class.java)
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
  
  fun loadRepositoryFromFile(file: Path): Repository {
    return loadRepositoryFromFile(file.toFile())
  }
  
  fun loadRepositoryFromFile(file: File): Repository {
    val mapper = if (file.extension.lowercase() == "json") DataUtils.jsonMapper() else DataUtils.yamlMapper()
    val repo = mapper.readValue(file, Repository::class.java)
    
    val result = Validator().validateRepo(repo)
    if (!result.isOk()) {
      throw ValidationError(result)
    }
    return repo
  }
  
  fun saveToFile(repo: Repository, file: File, format: String) {
    val mapper = if (format.uppercase() == "JSON") {
      DataUtils.jsonMapper()
    } else {
      DataUtils.yamlMapper()
    }
    
    repo.bundles.forEach { b ->
      b.versions.forEach { v ->
        v.entries.forEach { e ->
          if (!e.group) {
            e.selected = e.state != ItemState.SKIPPED
          }
        }
      }
    }
    
    mapper.writeValue(file, repo)
    
    val comments =
      DataUtils.PROPS_CACHE.computeIfAbsent("comments.properties") { DataUtils.loadPropsFromResources(it) }
    
    val tempFile = Files.createTempFile("ShvFileMatcher_repo_", ".tmp")
    Files.copy(file.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING)
    DataUtils.appendCommentsToYamlFile(
      tempFile.toFile(),
      file,
      comments,
      false
    )
  }
}
