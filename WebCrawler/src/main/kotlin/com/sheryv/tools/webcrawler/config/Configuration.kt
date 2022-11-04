package com.sheryv.tools.webcrawler.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.BrowserTypes
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.tools.webcrawler.utils.Utils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

class Configuration(
  val browserSettings: BrowserSettings = BrowserSettings(),
  var crawler: String? = null,
  var lastUserScript: String = "",
  @JsonDeserialize(using = SettingsSetDeserializer::class)
  val settings: MutableSet<SettingsBase>
) {
  
  @get:JsonProperty(index = -100)
  var modifyDate: OffsetDateTime = OffsetDateTime.now()
    private set
//  var settings: MutableMap<String, SettingsBase> = settings
//    private set
  
  fun updateSettings(settings: SettingsBase): Configuration {
    if (this.settings.contains(settings)) {
      this.settings.remove(settings)
    }
    this.settings.add(settings)
    return this
  }
  
  fun save(): Configuration {
    modifyDate = OffsetDateTime.now()
    mapper.writeValue(File(FILE), this)
    return this
  }
  
  companion object {
    const val FILE = "config.json"
    private const val PROP_FILE = "app.properties"
    
    @JvmStatic
    private val mapper by lazy {
      Utils.createJsonMapper()
    }
    
    @JvmStatic
    private val instance: Configuration by lazy(::load)
    
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
    private val properties: Map<String, String> by lazy {
      val p = Properties()
      p.load(Configuration::class.java.classLoader.getResourceAsStream(PROP_FILE))
      val map = p.map { it.key.toString() to it.value.toString() }.toMap().toMutableMap()
      System.getProperties().forEach { k, v -> map[k.toString()] = v.toString() }
      map
    }
    
    @JvmStatic
    private fun load(): Configuration {
      val path = Paths.get(FILE)
      return try {
        if (Files.exists(path)) {
          mapper.readValue(path.toFile(), Configuration::class.java).save()
        } else {
          Configuration(settings = Registry.get().crawlers().map { it.createDefaultSettings() }.toMutableSet()).save()
        }
      } catch (e: Exception) {
        throw AppError("Cannot load configuration file from '${path.toAbsolutePath()}'. Incorrect format", e)
      }
    }
  }
  
  
  fun copy(
    browserSettings: BrowserSettings = this.browserSettings.copy(),
    scrapper: String? = this.crawler,
    lastUserScript: String = this.lastUserScript,
    settings: MutableSet<SettingsBase> = this.settings.toMutableSet()
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

private class SettingsSetDeserializer : StdDeserializer<Set<SettingsBase>>(Set::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<SettingsBase> {
    val registry = Registry.get().crawlers()
    val mapping = registry.associate { it.id() to it.settingsClass }
    
    val settings =
      p.codec.readTree<JsonNode>(p).map { p.codec.treeToValue(it, mapping[it.get(SettingsBase.ID_FIELD_NAME).asText()]) }.toMutableSet()
    
    settings.addAll(mapping.keys.filterNot { id -> settings.any { it.crawlerId == id } }
      .map { id -> registry.first { it.id() == id }.createDefaultSettings() })
    
    return settings


//
//    val read = setAsTree.fields().asSequence().map {
//    val v = it.value as ObjectNode
//    v.put("", "")
//
//      it.key to p.codec.treeToValue(it.value., mapping[it.key])
//    }.toMap()
//    return registry.associate { it.attributes.id to read.getOrElse(it.attributes.id) { it.createDefaultSettings() } }
  }
  
}
