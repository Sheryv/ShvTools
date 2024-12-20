package com.sheryv.tools.webcrawler.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import java.nio.file.Path

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class SettingsBase(
) {
  abstract val crawlerId: String
  abstract val outputPath: Path
  
  protected val crawlerAttr: CrawlerAttributes by lazy { Registry.get().crawlers().first { it.id() == crawlerId }.attributes }
  
  abstract fun copyAll(): SettingsBase
  
  abstract fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader>
  
  open fun validate(def: CrawlerDef) {
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SettingsBase) return false
    
    if (crawlerId != other.crawlerId) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return crawlerId.hashCode()
  }
  
  override fun toString(): String {
    return "Settings($crawlerId)"
  }
  
  
  companion object {
    const val ID_FIELD_NAME = "crawlerId"
  }
  
  
}

//private class SettingsDeserializer : StdDeserializer<SettingsBase>(SettingsBase::class.java) {
//  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, SettingsBase> {
//    val registry = CrawlerRegistry.DEFAULT.all()
//    val mapping = registry.associate { it.attributes.id to it.settingsClass }
//
//    val tree = p.codec.readTree<JsonNode>(p)
//    val read = tree.fields().asSequence().map { it.key to p.codec.treeToValue(it.value, mapping[it.key]) }.toMap()
//    return registry.associate { it.attributes.id to read.getOrElse(it.attributes.id) { it.createDefaultSettings() } }
//  }
//
//}
