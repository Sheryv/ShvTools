package com.sheryv.tools.filematcher.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import javafx.stage.Window
import java.io.File
import java.net.SocketException
import java.nio.file.*

class RepositoryService {
  
  fun loadRepositoryConfig(url: String): Repository {
    return loadRepositoryFromTemplate(DataUtils.downloadAndParse(url, RepositoryTemplate::class.java))
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
    return loadRepositoryFromTemplate(mapper.readValue(file, RepositoryTemplate::class.java))
  }
  
  private fun loadRepositoryFromTemplate(template: RepositoryTemplate): Repository {
    val baseUrl = template.baseUrl
    val bundles = template.bundles.map { b ->
      val base = if (b.isLink()) {
        val link = b.toLink()
        try {
          val url = buildUrl(link.link, baseUrl)
          DataUtils.downloadAndParse(url, BundleTemplate::class.java).apply {
            id = link.id
            name = link.name.ifBlank { this.name }
            specSource = url
          }
        } catch (e: Exception) {
          throw ValidationError(
            e.message ?: "Cannot process external Bundle ${link.name} [${link.id}]. From: ${link.link}", e
          )
        }
      } else b
      
      val versions = base.versions?.map { ver ->
        if (ver is BundleVersionLink) {
          val url = buildUrl(ver.link, baseUrl)
          val externalVersion = loadExternalVersion(url)
          externalVersion.specSource = url
          externalVersion.copy(
            versionId = ver.versionId,
            versionName = ver.versionName.takeIf { !it.isNullOrBlank() } ?: externalVersion.versionName!!
          )
        } else {
          ver as BundleVersion
        }
      } ?: throw ValidationError(
        "No version found for bundle ${base.name} [${base.id}]${
          base.link?.let { " from URL: $it" }.orEmpty()
        }"
      )
      
      base.toBundle(versions)
    }
    
    val repo = template.toRepo(bundles)
    Validator().validateRepo(repo).throwIfError()
    return repo
  }
  
  private fun loadExternalVersion(url: String): BundleVersion {
    try {
      return DataUtils.downloadAndParse(url, BundleVersion::class.java)
    } catch (e: SocketException) {
      throw ValidationError("Cannot download external version from: $url", e)
    } catch (e: Exception) {
      throw ValidationError("Provided repository->bundle->version specification is incorrect. From: $url", e)
    }
  }
  
  private fun buildUrl(part: String, base: String?): String {
    if (DataUtils.isAbsoluteUrl(part)) {
      return part;
    }
    return base?.trimEnd('/') + "/" + SystemUtils.encodeNameForWeb(part.trim())
  }
  
  fun saveToFile(context: DevContext, file: File, options: SaveOptions): DevContext {
    val mapper = if (options.isJson()) {
      DataUtils.jsonMapper()
    } else {
      DataUtils.yamlMapper()
    }
    
    var repo = context.repoCopy
    val matchedSourceBundle = repo.bundles.firstOrNull { it.id == options.bundleId }
    
    val version = createVersion(context, options)
    val resultBundles = if (matchedSourceBundle != null) {
      if (options.overrideExistingItems) {
        //override
        repo.bundles.map {
          if (matchedSourceBundle.id == it.id) {
            val matchedVersion = matchedSourceBundle.versions.firstOrNull { it.versionId == options.versionId }
            
            val versions = if (matchedVersion != null) {
              matchedSourceBundle.versions.map {
                if (it.versionId == options.versionId) {
                  version
                } else {
                  it
                }
              }
            } else {
              matchedSourceBundle.versions.toMutableList().apply { add(version) }
            }
            
            createBundle(context, options, versions)
          } else {
            it
          }
        }
        
      } else {
        throw ValidationError(
          "Bundle with id ${options.bundleId} already exists and overriding is forbidden." +
              " Tick 'Override existing files/items while saving' in Options tab to allow overriding."
        )
      }
    } else {
      val bundle: Bundle = createBundle(context, options, listOf(version))
      context.repo.bundles.toMutableList().apply { add(bundle) }
    }
    repo = repo.copy(
      codeName = options.repoName,
      baseUrl = options.repoUrl,
      title = options.repoTitle,
      updateDate = Utils.now(),
      bundles = resultBundles,
    )
    
    Validator().validateRepo(repo).throwIfError()
    val template = RepositoryTemplate(repo)
    
    if (options.splitVersionsToFiles) {
      splitVersionsAndSaveToFiles(template, file, mapper)
    }
    
    mapper.writeValue(file, template)
    
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
    Files.deleteIfExists(tempFile)
    return DevContext(repo, version)
  }
  
  private fun createBundle(
    context: DevContext,
    overrides: SaveOptions,
    versions: List<BundleVersion>
  ) = context.bundle.copy(
    id = overrides.bundleId,
    name = overrides.bundleName,
    baseItemUrl = overrides.bundleUrl,
    preferredBasePath = BasePath(overrides.bundleBasePath),
    updateDate = Utils.now(),
    versions = versions,
  )
  
  private fun createVersion(
    context: DevContext,
    overrides: SaveOptions
  ): BundleVersion {
    return context.version.copy(
      versionId = overrides.versionId,
      versionName = overrides.versionName,
      versionUrlPart = SystemUtils.encodeNameForWeb(overrides.versionName),
      updateDate = Utils.now()
    ).apply {
      entries.forEach { e ->
        if (!e.group) {
          e.selected = e.state != ItemState.SKIPPED
        }
      }
    }
  }
  
  private fun splitVersionsAndSaveToFiles(
    template: RepositoryTemplate,
    repoFile: File,
    mapper: ObjectMapper
  ) {
    template.bundles.forEach { b ->
      val versions = mutableListOf<BundleVersionLink>()
      
      b.versions?.forEach { v ->
        var name = SystemUtils.removeForbiddenFileChars(b.name)
        
        if (name.isBlank()) {
          name = "Bundle-" + b.id
        }
        name += "_${v.versionName}.${repoFile.extension}"
        name = SystemUtils.removeForbiddenFileChars(name)
        mapper.writeValue(File(repoFile.parent, name), v)
        
        val l = BundleVersionLink(v.versionId, name, v.versionName)
        versions.add(l)
      }
      
      b.versions = versions
    }
  }
  
}
