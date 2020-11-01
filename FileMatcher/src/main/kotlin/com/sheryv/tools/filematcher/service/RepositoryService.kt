package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.ValidationError
import com.sheryv.tools.filematcher.utils.BundleUtils

class RepositoryService {
  
  fun loadRepositoryConfig(url: String): Repository {
    val repo = BundleUtils.nginxExample()
    val result = Validator().validateRepo(repo)
    if (!result.isOk()) {
      throw ValidationError(result)
    }
    return repo
  }
  
  fun synchronizeFiles(repo: Repository) {
  
  }
}