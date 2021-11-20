package com.sheryv.tools.websitescrapper.process.filmweb

import java.time.LocalDate

class FilmwebSearch(
  val id: Long,
  val lang: String? = null,
  val poster: String? = null,
  val release: LocalDate? = null,
  val title: String,
  val polishTitle: String? = null,
  typeNum: Int?,
  val url: String,
  val voteAverage: Double? = null,
  val voteCount: Long? = null,
  val rate: FilmwebSearchRate,
  val type: FilmwebRecordType = FilmwebRecordType.values().first { it.value == typeNum },
  val globalWantToSeeCount: Long?
) {

}
