package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import com.sheryv.util.Strings
import java.io.File
import java.time.format.DateTimeFormatter

class RepositorySaver(
  private val context: DevContext,
  private val file: File,
  private val options: SaveOptions,
  onFinish: ((ProcessResult<DevContext, RepositorySaver>) -> Unit)? = null
) : Process<DevContext>(onFinish as ((ProcessResult<DevContext, out Process<DevContext>>) -> Unit)?, false) {
  
  
  override suspend fun process(): DevContext {
    val service = RepositoryService()
    return service.saveToFile(
      context,
      file,
      options
    )
  }
}
