package com.sheryv.tools.filematcher.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import java.io.File
import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class RepositoryService {
  
  fun loadRepositoryConfig(url: String): Repository {
    return loadRepositoryFromTemplate(DataUtils.downloadAndParse(url, RepositoryTemplate::class.java))
  }
  
  fun loadRepositoryFromFile(file: Path): Repository {
    return loadRepositoryFromFile(file.toFile())
  }
  
  fun loadRepositoryFromFile(file: File): Repository {
    val mapper = if (file.extension.lowercase() == "json") DataUtils.jsonMapper() else DataUtils.yamlMapper()
    return loadRepositoryFromTemplate(mapper.readValue(file, RepositoryTemplate::class.java), file.toPath())
  }
  
  private fun loadRepositoryFromTemplate(template: RepositoryTemplate, baseDir: Path? = null): Repository {
    val repoBaseUrl = template.baseUrl
    val bundles = template.bundles.map { b ->
      val base = if (b.isLink()) {
        val link = b.toLink()
        try {
          val url = buildUrl(link.link, repoBaseUrl)
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
          var path: Path? = null
          if (!DataUtils.isAbsoluteUrl(ver.link)) {
            path = if (!DataUtils.isAbsoluteUrl(base.baseItemUrl.orEmpty())) {
              baseDir?.parent?.resolve(base.baseItemUrl.orEmpty())?.resolve(ver.link)
            } else {
              baseDir?.parent?.resolve(ver.link)
            } ?: Path.of(ver.link)
          }
          
          if (path != null && Files.exists(path)) {
            DataUtils.yamlMapper().readValue(path.toFile(), BundleVersion::class.java)
          } else {
            val url = buildUrl(ver.link, DataUtils.buildUrlFromBase(base.baseItemUrl, repoBaseUrl))
            val externalVersion = loadExternalVersion(url)
            externalVersion.specSource = url
            externalVersion.copy(
              versionId = ver.versionId,
              versionName = ver.versionName.takeIf { !it.isBlank() } ?: externalVersion.versionName
            )
          }
        } else {
          ver as BundleVersion
        }
      } ?: throw ValidationError(
        "No version found for bundle ${base.name} [${base.id}]${
          base.link?.let { " from URL: $it" }.orEmpty()
        }"
      )
      versions.forEach { ver ->
        ver.entries = ver.entries.map { if (it.updateDate == null) it.copy(updateDate = it.itemDate) else it }
      }
      base.toBundle(versions)
    }
    
    val repo = template.toRepo(bundles)
    Validator().validateRepo(repo).throwIfError()
    return repo
  }
  
  private fun loadExternalVersion(url: String): BundleVersion {
    try {
      return DataUtils.downloadAndParse(url, BundleVersion::class.java)
    } catch (e: Exception) {
      when (e) {
        is SocketException, is FileNotFoundException -> throw ValidationError("Cannot download external version from: $url", e)
      }
      throw ValidationError("Provided repository->bundle->version specification is incorrect. From: $url", e)
    }
  }
  
  private fun buildUrl(part: String, base: String?): String {
    if (DataUtils.isAbsoluteUrl(part)) {
      return part
    }
    return base?.trimEnd('/') + "/" + part.trim().trim('/')
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
  ): Bundle {
    val changed = context.bundle.versions.any { c -> c.updateDate != versions.firstOrNull { it.versionId == c.versionId }?.updateDate }
    val date = if (overrides.differsWithBundle(context.bundle) || changed) {
      Utils.now()
    } else {
      context.bundle.updateDate
    }
    
    return context.bundle.copy(
      id = overrides.bundleId,
      name = overrides.bundleName,
      baseItemUrl = overrides.bundleUrl,
      preferredBasePath = BasePath(overrides.bundleBasePath),
      updateDate = date,
      versions = versions,
    )
  }
  
  private fun createVersion(
    context: DevContext,
    overrides: SaveOptions
  ): BundleVersion {
    val date = if (overrides.differsWithVersion(context.version)
      || context.version.entries.filter { !it.group }.any { it.updateDate != it.previousUpdateDate }
    ) {
      Utils.now()
    } else {
      context.bundle.updateDate
    }
    
    
    return context.version.copy(
      versionId = overrides.versionId,
      versionName = overrides.versionName,
      versionUrlPart = SystemUtils.encodeNameForWeb(overrides.versionName),
      updateDate = date
    ).apply {
      val noDisabled = entries.filter { it.group || (!it.group && it.enabled) }
      
      entries.asSequence().filter { p -> p.group && !p.enabled && noDisabled.any { it.parent == p.id } }
        .forEach { it.enabled = true; it.updateBindings() }
      val list = entries.toMutableList()
      list.removeIf { p -> p.group && entries.none { it.parent == p.id } }
      entries = list
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
        val l = when (v) {
          is BundleVersion -> {
            var name: String
            
            var path = File(repoFile.parent)
            
            if (!DataUtils.isAbsoluteUrl(b.baseItemUrl.orEmpty()))
              path = path.resolve(b.baseItemUrl.orEmpty())
            
            if (v.sourceLink != null
              && !DataUtils.isAbsoluteUrl(v.sourceLink!!.link)
              && v.sourceLink!!.versionId == v.versionId
              && v.sourceLink!!.versionName == v.versionName
            ) {
              name = v.sourceLink!!.link
            } else {
              name = SystemUtils.removeForbiddenFileChars(b.id)
              if (name.isBlank()) {
                name = "Bundle-" + b.id
              }
              name += "_${v.versionName}.${repoFile.extension}"
              name = SystemUtils.removeForbiddenFileChars(name.replace(' ', '_'))
              name = SystemUtils.encodeNameForWeb(name)
              //        if (b.baseItemUrl != null && !DataUtils.isAbsoluteUrl(b.baseItemUrl!!)) {
              //          name = Path.of(b.baseItemUrl!!).joinToString("/") + name
              //        }
            }
            
            path = path.resolve(name)
            path.parentFile.mkdirs()
            
            mapper.writeValue(path, v)
            
            BundleVersionLink(v.versionId, name, v.versionName)
          }
          
          is BundleVersionLink -> v
          else -> throw RuntimeException("unknown version")
        }
        versions.add(l)
      }
      
      b.versions = versions
    }
  }
  
}
