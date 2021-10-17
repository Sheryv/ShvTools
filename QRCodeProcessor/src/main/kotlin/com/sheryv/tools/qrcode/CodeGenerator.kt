package com.sheryv.tools.qrcode

import java.awt.image.BufferedImage

fun interface CodeGenerator {
  fun generate(input: String, format: Format, size: Int): BufferedImage
}
