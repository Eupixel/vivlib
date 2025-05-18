package net.eupixel.vivlib.util

import net.minestom.server.coordinate.Pos
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

    fun convertToPos(raw: String?): Pos {
        if (raw.isNullOrBlank() || !raw.contains("#")) return Pos.ZERO
        val parts = raw.split("#")
        return try {
            when (parts.size) {
                3 -> {
                    val x = parts[0].toDouble()
                    val y = parts[1].toDouble()
                    val z = parts[2].toDouble()
                    Pos(x, y, z)
                }
                5 -> {
                    val x = parts[0].toDouble()
                    val y = parts[1].toDouble()
                    val z = parts[2].toDouble()
                    val yaw = parts[3].toFloat()
                    val pitch = parts[4].toFloat()
                    Pos(x, y, z, yaw, pitch)
                }
                else -> Pos.ZERO
            }
        } catch (_: NumberFormatException) {
            Pos.ZERO
        }
    }
}