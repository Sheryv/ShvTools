package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

enum class EpisodeAudioTypes(val priority: Int) {
  LECTOR(0), DUBBING(1), SUBS(2), SUBS_GENERATED(5), ORIGIN(10), UNKNOWN(100)
}
