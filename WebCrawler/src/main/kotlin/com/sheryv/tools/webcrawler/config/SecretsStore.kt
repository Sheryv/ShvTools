package com.sheryv.tools.webcrawler.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

private const val FILE_NAME = "secrets.properties"
private const val TEMPLATE_FILE_NAME = "secrets.template.properties"

class SecretsStore(
  val tmdbApiKey: Secret,
  val traktClient: Secret,
  val traktSecret: Secret,
  val traktToken: Secret,
  val traktRefreshToken: Secret,
  val openSubtitlesApiKey: Secret,
  val openSubtitlesUsername: Secret,
  val openSubtitlesPassword: Secret,
) {
  
  companion object {
    
    private fun loadSingle(props: Properties, key: String): Secret {
      return Secret(props.getProperty(key, "").takeIf { it.isNotBlank() }, key)
    }
    
    fun load(): SecretsStore {
      val secrets = Path.of("secrets.properties")
      val props = Properties()
      if (secrets.exists()) {
        props.load(Files.newBufferedReader(secrets))
      } else {
        javaClass.getResourceAsStream(TEMPLATE_FILE_NAME)?.use { it.readAllBytes() }?.also {
          Files.write(secrets, it)
        }
      }
      
      return SecretsStore(
        tmdbApiKey = loadSingle(props, "TMDB_API_KEY"),
        traktClient = loadSingle(props, "TRAKT_CLIENT"),
        traktSecret = loadSingle(props, "TRAKT_SECRET"),
        traktToken = loadSingle(props, "TRAKT_TOKEN"),
        traktRefreshToken = loadSingle(props, "TRAKT_REFRESH_TOKEN"),
        openSubtitlesApiKey = loadSingle(props, "OPEN_SUBTITLES_API_KEY"),
        openSubtitlesUsername = loadSingle(props, "OPEN_SUBTITLES_USERNAME"),
        openSubtitlesPassword = loadSingle(props, "OPEN_SUBTITLES_PASSWORD")
      )
    }
  }
  
  
  data class Secret(val value: String?, private val name: String) {
    fun require(): String {
      if (value?.isNotBlank() ?: false) {
        return value
      }
      throw RuntimeException(
        "Cannot find secret '$name'. Make sure you have configured value for $name " +
            "in the file '$FILE_NAME' that is located in the application directory"
      )
    }
  }
  
}
