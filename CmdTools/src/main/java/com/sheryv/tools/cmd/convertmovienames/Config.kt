package com.sheryv.tools.cmd.convertmovienames

class Config(
  val tmdbKey: String = "",
  val searchMovies: Boolean = true
) {
  
  fun validate() {
    require(tmdbKey.isNotBlank()) { "TMDB key cannot be empty" }
  }
}
