package com.sheryv.tools.webcrawler.service.streamingwebsite.moviesearch

import java.nio.file.Path

data class MovieFromFile(val title: String, val id: Int, val path: Path, val year: Int)
