package com.sheryv.tools.webcrawler.service.subsdownload

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipUtils {
  @Throws(IOException::class)
  fun unzip(zip: File, destinationDir: File) {
    val buffer = ByteArray(8 * 1024)
    val zis = ZipInputStream(FileInputStream(zip))
    var zipEntry = zis.getNextEntry()
    destinationDir.mkdirs()
    while (zipEntry != null) {
      val newFile = newFile(destinationDir, zipEntry)
      newFile.getParentFile().mkdirs()
      val fos = FileOutputStream(newFile)
      var len: Int
      while ((zis.read(buffer).also { len = it }) > 0) {
        fos.write(buffer, 0, len)
      }
      fos.close()
      zipEntry = zis.getNextEntry()
    }
    zis.closeEntry()
    zis.close()
  }
  
  @Throws(IOException::class)
  private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
    val destFile = File(destinationDir, zipEntry.getName())
    
    val destDirPath = destinationDir.getCanonicalPath()
    val destFilePath = destFile.getCanonicalPath()
    
    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw IOException("Entry is outside of the target dir: " + zipEntry.getName())
    }
    
    return destFile
  }
}
