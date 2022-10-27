package com.sheryv.tools.websitescraper.config.impl.streamingwebsite

import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.EpisodeTypes

data class EpisodeType(val kind: EpisodeTypes, val enabled: Boolean = true)
