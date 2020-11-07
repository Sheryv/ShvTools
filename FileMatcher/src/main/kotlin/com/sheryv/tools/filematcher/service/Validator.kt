package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import java.net.URL

class Validator {
  
  fun url(url: String): ValidationResult {
    try {
      URL(url)
    } catch (e: Exception) {
      return ValidationResult(e.message!!)
    }
    return ValidationResult()
  }
  
  fun validateRepo(repo: Repository): ValidationResult {
    val result = ValidationResult()
        .assert(repo.codeName.isNotBlank(), "Field 'codeName' in object 'Repository' cannot be empty.")
        .assert(repo.title.isNotBlank(), "Field 'title' in object 'Repository' cannot be empty.")
        .assert(repo.schemaVersion == 1L, "Field 'schemaVersion' in object 'Repository' cannot be empty. Currently supported version of schema is '1'. Check for updates to get support for newer versions.")
        .assert(inRange(repo.title, 100), "Field 'title' in object 'Repository' cannot be longer than 100 chars.")
        .assert(inRange(repo.codeName, 50), "Field 'title' in object 'Repository' cannot be longer than 50 chars.")
        .assert(inRange(repo.repositoryVersion, 50), "Field 'version' in object 'Repository' cannot be longer than 50 chars.")
        .assert(inRange(repo.website, 1000), "Field 'website' in object 'Repository' cannot be longer than 1000 chars.")
        .assert(inRange(repo.baseUrl, 1000), "Field 'baseUrl' in object 'Repository' cannot be longer than 1000 chars.")
        .assert(inRange(repo.author, 100), "Field 'website' in object 'Repository' cannot be longer than 100 chars.")
        .assert(repo.bundles.isNotEmpty(), "Field 'bundles' in object 'Repository' cannot be empty. Repository have to contain at least one bundle.")
    
    result.merge(validateAddFields(repo.additionalFields, "'Repository'"))
    result.merge(validateBundles(repo))
    result.merge(validateVersions(repo))
    result.merge(validateEntries(repo))
    return result
  }
  
  fun validateBundles(repo: Repository): ValidationResult {
    val ids = repo.bundles.map { it.id }
    
    val result = ValidationResult()
    repo.bundles.forEach { b ->
      val baseUrl = b.getBaseUrl(repo.baseUrl)
      result
          .assert(ids.count { it == b.id } == 1, "Field 'id' of object 'Bundle' has incorrect value, at [id=${b.id}, name=${b.name}]. Value '${b.id}' is duplicated in this repository.")
          .assert(b.name.isNotBlank(), "Field 'name' of object 'Bundle' cannot be empty, at [id=${b.id}, name=${b.name}].")
          .assert(baseUrl?.let { url(it).isOk() }, "Field 'baseItemUrl' of object 'Bundle' has incorrect value, at [id=${b.id}, name=${b.name}]. Concatenated url '${baseUrl}' is not valid url address.")
          .assert(b.versions.isNotEmpty(), "Field 'versions' of object 'Bundle' cannot be empty. Bundle have to contain at least one version, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.id, 50), "Field 'id' in object 'Bundle' cannot be longer than 50 chars, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.name, 100), "Field 'name' in object 'Bundle' cannot be longer than 100 chars, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.description, 6000), "Field 'description' in object 'Bundle' cannot be longer than 6000 chars, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.baseItemUrl, 1000), "Field 'baseItemUrl' in object 'Bundle' cannot be longer than 1000 chars, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.link, 1000), "Field 'link' in object 'Bundle' cannot be longer than 1000 chars, at [id=${b.id}, name=${b.name}].")
          .assert(inRange(b.linkedId, 50), "Field 'linkedId' in object 'Bundle' cannot be longer than 50 chars, at [id=${b.id}, name=${b.name}].")
      result.merge(validateAddFields(b.additionalFields, "'Bundle' at [id=${b.id}, name=${b.name}]"))
    }
    return result
  }
  
  fun validateVersions(repo: Repository): ValidationResult {
    val result = ValidationResult()
    repo.bundles.forEach { b ->
      
      val ids = b.versions.map { it.versionId }
      val idCodes = b.versions.map { it.versionName }
      b.versions.forEach { v ->
        result
            .assert(ids.count { it == v.versionId } == 1, "Field 'versionId' of Version has incorrect value, at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]. Value '${v.versionId}' is duplicated in this bundle.")
            .assert(idCodes.count { it == v.versionName } == 1, "Field 'versionName' of Version has incorrect value, at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]. Value '${v.versionName}' is duplicated in this bundle.")
            .assert(inRange(v.versionName, 50), "Field 'versionName' of Version cannot be longer than 50 chars, at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]")
            .assert(v.versionId >= 0, "Field 'versionId' of Version cannot be negative number, at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]")
            .assert(inRange(v.changesDescription, 6000), "Field 'changesDescription' of Version cannot be longer than 6000 chars, at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]")
        result.merge(validateAddFields(v.additionalFields, "Version at [bundle-id=${b.id}, bundle-name=${b.name}, version=${v.versionId}]"))
      }
    }
    return result
  }
  
  fun validateEntries(repo: Repository): ValidationResult {
    val result = ValidationResult()
    repo.bundles.forEach { b ->
      
      b.versions.forEach { v ->
        val ids = v.entries.map { it.id }
        result.merge(validateEntryList(v.entries, ids, repo, b, v))
      }
    }
    return result
  }
  
  fun validateEntryList(entries: List<Entry>, ids: List<String>, repo: Repository, bundle: Bundle, version: BundleVersion): ValidationResult {
    val result = ValidationResult()
    entries.forEach { e ->
      
      val srcUrl = e.getSrcUrl(bundle.getBaseUrl(repo.baseUrl))
      result
          .assert(ids.count { it == e.id } == 1, errorForEntry("id", "Value '${e.id}' is duplicated in this bundle.", e, bundle, version))
          .assert(inRange(e.name, 199), errorForEntry("name", "", e, bundle, version, "cannot be longer than 199 chars"))
          .assert(inRange(e.version, 100), errorForEntry("version", "", e, bundle, version, "cannot be longer than 100 chars"))
          .assert(inRange(e.website, 1000), errorForEntry("website", "", e, bundle, version, "cannot be longer than 1000 chars"))
          .assert(inRange(e.description, 6000), errorForEntry("description", "", e, bundle, version, "cannot be longer than 6000 chars"))
          .assert(!e.target.absolute || (e.target.absolute && e.target.path != null && e.target.path.findPath() != null
              && !e.target.path.findPath().isNullOrBlank()), errorForEntry("target", "When 'absolute=true' path cannot be empty.", e, bundle, version))
      
      
      SystemUtils.fileNameForbiddenChars().forEach {
        if (e.name.contains(it)) {
          result.addError(errorForEntry("name", "Name '${e.name}' contains forbidden character '$it', code: ${it.toInt()}.", e, bundle, version))
        }
      }
  
      if (e.parent != null) {
        val parent = entries.firstOrNull { it.id == e.parent }
        if (parent == null) {
          result.addError(errorForEntry("parent", "Parent item '${e.parent}' was not found.", e, bundle, version))
        } else if (!parent.group) {
          result.addError(errorForEntry("parent", "Parent item '${parent.name}' [id=${parent.id}] have to be group. 'group=true' is required. Defined by child [id=${e.id}]", e, bundle, version))
        }
        result.assert(e.parent != e.id, errorForEntry("parent", "Item cannot be parent of itself.", e, bundle, version, "is equal to 'id' of this item"))
      }
  
      if (e.group) {
        val children = BundleUtils.getFirstLevelChildren(e.id, entries)
        val s = mutableSetOf<String>()
        for (child in children) {
          if (!s.add(child.name)) {
            result.addError(errorForEntry("name", "Name '${child.name}' of item [id=${child.id}] is duplicated in group ${e.name}, ${e.id}.", e, bundle, version))
          }
        }
        result.assert(!e.target.absolute, errorForEntry("target", "When 'group=true' path.absolute cannot be true - absolute paths are not supported for groups.", e, bundle, version))
      } else {
        result
            .assert(srcUrl.let { url(it).isOk() }, errorForEntry("src", "Url '$srcUrl' is not valid.", e, bundle, version))
            .assert(inRange(srcUrl, 3000), errorForEntry("src", "Concatenated url '$srcUrl' is too long.", e, bundle, version, "cannot be longer than 1000 chars"))
      }
    }
    return result
  }
  
  private fun errorForEntry(field: String, details: String, e: Entry, bundle: Bundle, version: BundleVersion, type: String = "has incorrect value"): String {
    val ob = if (e.group) "Group" else "Item"
    return "Field '$field' of $ob $type, at [bundle-id=${bundle.id}, bundle-name=${bundle.name}, version=${version.versionId}, item-id=${e.id}, item-name=${e.name}]. " + details
  }
  
  fun validateAddFields(fields: Map<String, String?>, parent: String): ValidationResult {
    val result = ValidationResult()
        .assert(fields.size <= 50, "Field 'additionalFields' in type $parent cannot have more than 50 rows.")
    
    fields.forEach { (k, v) ->
      result.assert(k.isNotBlank(), "Keys for rows of field 'additionalFields' in type $parent cannot be empty.")
          .assert(v != null, "Values for rows of field 'additionalFields' in type $parent cannot be empty. Occurred at key: '$k'")
          .assert(k.length <= 100, "Keys for rows of field 'additionalFields' in type $parent cannot be longer than 100 chars.")
          .assert(inRange(v, 6000), "Values for rows of field 'additionalFields' in type $parent cannot be longer than 6000 chars.")
    }
    return result
  }
  
  private fun inRange(text: String?, max: Int = 100, min: Int = 0): Boolean {
    return text == null || !(text.length > max || text.length < min)
  }
}