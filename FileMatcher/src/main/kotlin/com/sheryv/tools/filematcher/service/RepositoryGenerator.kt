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

class RepositoryGenerator(
  private val options: SaveOptions,
  onFinish: ((ProcessResult<Repository, RepositoryGenerator>) -> Unit)? = null
) : Process<Repository>(onFinish as ((ProcessResult<Repository, out Process<Repository>>) -> Unit)?, false) {
  
  
  override suspend fun process(): Repository {
    return generateFromLocalDir(options)
  }
  
  private fun generateFromLocalDir(options: SaveOptions): Repository {
    val bundles = listOf(
      Bundle(
        options.bundleId, options.bundleName, preferredBasePath = BasePath(options.bundleBasePath),
        versions = listOf(
          BundleVersion(
            versionId = options.versionId,
            versionName = options.versionName,
            entries = emptyList(),
          )
        )
      )
    )
    
    return Repository(
      options.repoUrl, options.repoName, "1.0", 1,
      options.repoUrl,
      "Example title", "", "description", mapOf("param1" to 123.toString()), bundles = bundles
    )
  }
}
