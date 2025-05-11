package net.eupixel.vivlib.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object Helper {
    fun unzip(zipFilePath: String, outputDirPath: String) {
        ZipInputStream(File(zipFilePath).inputStream()).use { zipIn ->
            val targetDir = File(outputDirPath).apply { mkdirs() }
            var entry = zipIn.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfter('/', "")
                if (name.isNotEmpty()) {
                    val outFile = File(targetDir, name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile!!.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zipIn.copyTo(fos)
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
}