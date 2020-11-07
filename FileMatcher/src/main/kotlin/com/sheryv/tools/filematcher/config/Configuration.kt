package com.sheryv.tools.filematcher.config

import com.sheryv.util.SerialisationUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class Configuration {
  var findItemsByHash: Boolean = false
  var recentRepositories: MutableList<String> = LinkedList()
  var lastLoadedRepoFile: String? = null
  var devTools: DevToolConfig = DevToolConfig()
  
  fun save() {
    val s = SerialisationUtils.toJson(this)
    Files.writeString(Paths.get(FILE), s)
  }
  
  
  companion object {
    @JvmStatic
    private var instance: Configuration? = null
    private const val FILE = "config.json"
    
    @JvmStatic
    fun get(): Configuration {
      if (instance == null && Files.exists(Paths.get(FILE))) {
        instance = SerialisationUtils.fromJson(Files.readString(Paths.get(FILE)), Configuration::class.java)
      } else if (instance == null) {
        instance = Configuration()
        instance!!.save()
      }
      return instance!!
    }
  }
  
}

class DevToolConfig(
    var outputPath: String? = null,
    var sourcePath: String? = null,
    var mcCursePath: String? = null,
    var bundlePreferredPath: String? = null
) {

}