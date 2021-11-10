package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.ProcessResult
import com.sheryv.tools.filematcher.model.Repository
import java.nio.file.Path

class RepositoryFileLoader(private val file: Path, onFinish: ((ProcessResult<Repository, RepositoryGenerator>) -> Unit)? = null)
  : Process<Repository>(onFinish as ((ProcessResult<Repository, out Process<Repository>>) -> Unit)?, false) {
  
  override suspend fun process(): Repository {
    return RepositoryService().loadRepositoryFromFile(file)
  }
}
