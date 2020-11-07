package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.ValidationError
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import javafx.stage.Window
import java.io.File
import java.nio.file.Path

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
  
  fun loadRepositoryFromFile(file: Path): Repository? {
    val mapper = if (file.toFile().extension.toLowerCase() == "json") DataUtils.jsonMapper() else DataUtils.yamlMapper()
    val repo = mapper.readValue(file.toFile(), Repository::class.java)
    
    val result = Validator().validateRepo(repo)
    if (!result.isOk()) {
      throw ValidationError(result)
    }
    return repo
  }
  
  fun saveToFile(repo: Repository, file: File, format: String) {
    val mapper = if (format.toUpperCase() == "JSON") {
      DataUtils.jsonMapper()
    } else {
      DataUtils.yamlMapper()
    }
    
    mapper.writeValue(file, repo)
  }
}