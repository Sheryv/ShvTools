package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.lg
import java.io.File
import java.nio.file.Paths

class RepositoryHashUpdater(private val dir: File, private val repoPath: File, onFinish: ((ProcessResult<Repository, RepositoryHashUpdater>) -> Unit)? = null)
  : Process<Repository>(onFinish as ((ProcessResult<Repository, out Process<Repository>>) -> Unit)?, false) {
  
  
  override suspend fun process(): Repository {
    return updateHashes(dir)
  }
  
  
  private fun updateHashes(dir: File): Repository {
    val rootDir = if (!dir.isDirectory) {
      dir.parentFile
    } else {
      dir
    }
    
    val repository = RepositoryService().loadRepositoryFromFile(repoPath.toPath())!!
    
    repository.bundles.forEach { b ->
      b.versions.forEach { v ->
        val userContext = UserContext(repository, b.id, v.versionId, rootDir.absoluteFile)
        
        v.entries.forEach { e ->
          val entryDir = Paths.get(getEntryDir(userContext, e))
          val file = entryDir.resolve(e.name)
          if (!e.group) {
            if (file.toFile().exists()) {
              val md5 = Hashing.md5(file)
              e.hashes = e.hashes?.copy(md5 = md5) ?: Hash(md5)
            } else {
              lg().info("File does not exist: {}", file.toAbsolutePath())
            }
          }
        }
        
      }
      
    }
    return repository
  }
  
  private fun getEntryDir(context: UserContext, entry: Entry): String {
    return if (entry.target.absolute) {
      entry.target.path!!.findPath()!!
    } else {
      context.buildDirPathForEntry(entry).resolve(entry.target.path?.findPath() ?: "").toAbsolutePath().toString()
    }
  }
}