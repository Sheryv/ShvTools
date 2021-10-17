package com.sheryv.tools.qrcode

import java.nio.file.Path

fun interface CodeFileReader {
  fun read(path: Path): Map<Format, String>
}
