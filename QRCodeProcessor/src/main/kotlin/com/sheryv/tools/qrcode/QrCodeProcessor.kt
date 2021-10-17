package com.sheryv.tools.qrcode

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatLightLaf
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.Reader
import com.google.zxing.Result
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.absolutePathString


class QrCodeProcessor {
  
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      FlatLightLaf.setup()
      
      MainForm(
        { i, f, s ->
          if (f == Format.BASE64) {
            generateQRCodeImage(String(Base64.getDecoder().decode(i), StandardCharsets.UTF_8), s)
          } else {
            generateQRCodeImage(i, s)
          }
        },
        {
          val r = decodeQRImage(it.absolutePathString())
          mapOf(Format.PLAIN to r.text, Format.BASE64 to Base64.getEncoder().encodeToString(r.rawBytes))
        }
      )
    }
    
    @JvmStatic
    private fun generateQRCodeImage(barcodeText: String, size: Int): BufferedImage {
      val barcodeWriter = QRCodeWriter()
      val bitMatrix: BitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, size, size)
      return MatrixToImageWriter.toBufferedImage(bitMatrix)
    }
    

    @JvmStatic
    private fun decodeQRImage(path: String): Result {
      val binaryBitmap = BinaryBitmap(
        HybridBinarizer(
          BufferedImageLuminanceSource(
            ImageIO.read(FileInputStream(path))
          )
        )
      )
      
      val reader: Reader = QRCodeReader()
      val result = reader.decode(binaryBitmap)
      
      return result
    }
  }
}
