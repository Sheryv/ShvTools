package com.sheryv.tools.webcrawler.service.subsdownload

class SubsDownloadOptions(
  val urlFormatString: String? = null,
  val baseUrl: String? = null,
  val downloadDirectory: String? = null,
  val temporaryDirectory: String = "",
  val seriesId: String? = null,
  val series: String? = null,
  val season: Int = 0,
  val episode: Int = 0
) {
  
  fun formatFullUrl(): String {
    return baseUrl + String.format(urlFormatString!!, seriesId, season, episode)
  }
  
  companion object {
    fun default(): SubsDownloadOptions {
      val options = SubsDownloadOptions(
        baseUrl = "https://www.opensubtitles.org",
        temporaryDirectory = "C:\\temp",
        urlFormatString = "/pl/ssearch/sublanguageid-pol,eng/searchonlytvseries-on/season-%2\$d/episode-%3\$d/idmovie-%1\$s"
      )
      return options
    }
  }
}
