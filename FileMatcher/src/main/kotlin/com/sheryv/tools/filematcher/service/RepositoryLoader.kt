package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.ProcessResult
import com.sheryv.tools.filematcher.model.Repository

class RepositoryLoader(private val url: String, onFinish: ((ProcessResult<Repository, RepositoryGenerator>) -> Unit)? = null)
  : Process<Repository>(onFinish as ((ProcessResult<Repository, out Process<Repository>>) -> Unit)?, false) {
  
  override suspend fun process(): Repository {
    return RepositoryService().loadRepositoryConfig(url)
  }
}