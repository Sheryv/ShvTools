package com.sheryv.tools.webcrawler.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import com.sheryv.util.DateUtils
import java.nio.file.Path
import java.time.format.DateTimeFormatter

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class SettingsBase(
  val crawlerId: String,
  outputPath: Path? = null,
) {
  protected val crawlerAttr: CrawlerAttributes = Registry.get().crawlers().first { it.id() == crawlerId }.attributes
  
  open val outputPath: Path = outputPath ?: defaultOutputPath()
  
  abstract fun copy(
    crawlerId: String = this.crawlerId,
    outputPath: Path = this.outputPath,
  ): SettingsBase
  
  protected fun defaultOutputPath(): Path {
    return SystemSupport.get.userDownloadDir.resolve(
      "${SystemSupport.get.removeForbiddenFileChars(crawlerAttr.id)}-" +
          "${DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.${crawlerAttr.outputFileFormat.extension}"
    ).toAbsolutePath()
  }
  
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
