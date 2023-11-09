package com.sheryv.tools.filematcher.config

import com.sheryv.tools.filematcher.model.SaveOptions
import com.sheryv.tools.filematcher.model.ValidationError
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.core.app.AppConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class Configuration : AppConfiguration() {
  var recentRepositories: MutableList<String> = LinkedList()
  var lastLoadedRepoFile: String? = null
  var devTools: DevToolConfig = DevToolConfig()
  
  @Transient
  override val name: String = ViewUtils.title
  @Transient
  override val iconPath: String = "icons/app.png"
  
  fun save() {
    SerialisationUtils.toJson(File(FILE), this)
  }
  
  
  companion object {
    @JvmStatic
    private var instance: Configuration? = null
    private const val FILE = "config.json"
    
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
        throw ValidationError("Cannot load configuration file from '${path.toAbsolutePath()}'. Incorrect format", e)
      }
    }
  }
  
}

class DevToolConfig(
  var outputPath: String? = null,
  var sourcePath: String? = null,
  var mcCursePath: String? = null,
  var options: SaveOptions = BundleUtils.defaultOptions(),
) {

}
