package com.sheryv.tools.webcrawler.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.BrowserTypes
import com.sheryv.tools.webcrawler.process.ScraperRegistry
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.tools.webcrawler.utils.Utils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

class Configuration(
  val browserSettings: BrowserSettings = BrowserSettings(),
  var scrapper: String? = null,
  var lastUserScript: String = "",
  @JsonDeserialize(using = SettingsDeserializer::class)
  val settings: MutableMap<String, SettingsBase>
) {
  
  @get:JsonProperty(index = -100)
  var modifyDate: OffsetDateTime = OffsetDateTime.now()
    private set
//  var settings: MutableMap<String, SettingsBase> = settings
//    private set
  
  fun save(): Configuration {
    modifyDate = OffsetDateTime.now()
    mapper.writeValue(File(FILE), this)
    return this
  }
  
  companion object {
    @JvmStatic
    private val mapper by lazy {
      Utils.createJsonMapper(ScraperRegistry.DEFAULT.all().associate { it.id to it.settingsClass })
    }
    
    
    @JvmStatic
    private val instance: Configuration by lazy(::load)
    
    const val FILE = "config.json"
    private const val PROP_FILE = "app.properties"
    
    @JvmStatic
    private val properties: Map<String, String> by lazy {
      val p = Properties()
      p.load(Configuration::class.java.classLoader.getResourceAsStream(PROP_FILE))
      val map = p.map { it.key.toString() to it.value.toString() }.toMap().toMutableMap()
      System.getProperties().forEach { k, v -> map[k.toString()] = v.toString() }
      map
    }
    
    @JvmStatic
    fun get(): Configuration = instance
    
    @JvmStatic
    fun props(): Map<String, String> {
      return properties
    }
    
    @JvmStatic
    fun property(key: String): String? {
      return properties[key]
    }
    
    @JvmStatic
    private fun load(): Configuration {
      val path = Paths.get(FILE)
      return try {
        if (Files.exists(path)) {
          val config = mapper.readValue(path.toFile(), Configuration::class.java)
//          ScraperRegistry.DEFAULT.all()
//            .map { it.id to it.createDefaultSettings() }
//            .filterNot { def -> current.containsKey(def.first) }
//            .forEach { current.add(it) }
          config.save()
        } else {
          Configuration(settings = ScraperRegistry.DEFAULT.all().associate { it.id to it.createDefaultSettings() }.toMutableMap()).save()
        }
      } catch (e: Exception) {
        throw AppError("Cannot load configuration file from '${path.toAbsolutePath()}'. Incorrect format", e)
      }
    }
  }
  
  
  fun copy(
    browserSettings: BrowserSettings = this.browserSettings.copy(),
    scrapper: String? = this.scrapper,
    lastUserScript: String = this.lastUserScript,
    settings: MutableMap<String, SettingsBase> = this.settings.toMutableMap()
  ): Configuration {
    return Configuration(browserSettings, scrapper, lastUserScript, settings)
  }
}

data class BrowserSettings(
  @JsonDeserialize(`as` = LinkedHashSet::class)
  val configs: Set<BrowserConfig> = BrowserConfig.all(),
  var selected: BrowserTypes = configs.first().type,
  var useUserProfile: Boolean = true,
) {
  fun currentBrowser() = configs.first { it.type == selected }
}

private class SettingsDeserializer : StdDeserializer<Map<String, SettingsBase>>(Map::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, SettingsBase> {
    val registry = ScraperRegistry.DEFAULT.all()
    val mapping = registry.associate { it.id to it.settingsClass }
    
    val tree = p.codec.readTree<JsonNode>(p)
    val read = tree.fields().asSequence().map { it.key to p.codec.treeToValue(it.value, mapping[it.key]) }.toMap()
    return registry.associate { it.id to read.getOrElse(it.id) { it.createDefaultSettings() } }
  }
  
}
