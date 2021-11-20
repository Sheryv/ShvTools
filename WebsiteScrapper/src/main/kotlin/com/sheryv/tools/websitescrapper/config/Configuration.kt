package com.sheryv.tools.websitescrapper.config

import com.sheryv.tools.websitescrapper.browser.BrowserType
import com.sheryv.tools.websitescrapper.browser.DriverType
import com.sheryv.tools.websitescrapper.utils.AppError
import com.sheryv.util.SerialisationUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class Configuration(
  var savePath: String? = null,
  var browser: BrowserType? = null,
  var browserPath: String? = null,
  var browserDriverPath: String? = null,
  var browserDriverType: DriverType? = null,
  var scrapper: String? = null,
  var useUserProfile: Boolean? = null,
) {
  
  fun save() {
    SerialisationUtils.toJson(File(FILE), this)
  }
  
  companion object {
    @JvmStatic
    private var instance: Configuration? = null
    private const val FILE = "config.json"
    private const val PROP_FILE = "app.properties"
    @JvmStatic
    private val properties: Map<String, String> by lazy {
      val p = Properties()
      p.load(Configuration::class.java.classLoader.getResourceAsStream(PROP_FILE))
      p.map { it.key.toString() to it.value.toString() }.toMap()
    }
    
    @JvmStatic
    fun get(): Configuration {
      val path = Paths.get(FILE)
      try {
        if (instance == null && Files.exists(path)) {
          instance = SerialisationUtils.fromJson(path.toFile(), Configuration::class.java)
        } else if (instance == null) {
          instance = Configuration()
          instance!!.save()
        }
        return instance!!
      } catch (e: Exception) {
        throw AppError("Cannot load configuration file from '${path.toAbsolutePath()}'. Incorrect format", e)
      }
    }
  
    @JvmStatic
    fun props(): Map<String, String> {
      return properties
    }
  
    @JvmStatic
    fun property(key: String): String? {
      return properties[key]
    }
  }
  
  
  fun copy(
    savePath: String? = this.savePath,
    browser: BrowserType? = this.browser,
    browserPath: String? = this.browserPath,
    browserDriverPath: String? = this.browserDriverPath,
    browserDriverType: DriverType? = this.browserDriverType,
    scrapper: String? = this.scrapper,
    useUserProfile: Boolean? = this.useUserProfile,
    ): Configuration {
    return Configuration(savePath, browser, browserPath, browserDriverPath, browserDriverType, scrapper, useUserProfile)
  }
}
