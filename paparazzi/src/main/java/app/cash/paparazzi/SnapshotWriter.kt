/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi

import app.cash.paparazzi.SnapshotHandler.FrameHandler
import app.cash.paparazzi.internal.apng.ApngWriter
import okio.HashingSink
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import java.awt.image.BufferedImage
import java.io.File

public class SnapshotWriter @JvmOverloads constructor(
  rootDirectory: File = File(System.getProperty("paparazzi.report.dir"))
) : SnapshotHandler {
  private val imagesDirectory: File = File(rootDirectory, "images")
  private val videosDirectory: File = File(rootDirectory, "videos")

  private val goldenDirectory = File(rootDirectory, "golden")
  private val goldenImagesDirectory = File(goldenDirectory, "images")
  private val goldenVideosDirectory = File(goldenDirectory, "videos")

  init {
    imagesDirectory.mkdirs()
    videosDirectory.mkdirs()
    goldenImagesDirectory.mkdirs()
    goldenVideosDirectory.mkdirs()
  }

  override fun newFrameHandler(
    snapshot: Snapshot,
    frameCount: Int,
    fps: Int
  ): FrameHandler {
    return object : FrameHandler {
      val snapshotDir = if (fps == -1) imagesDirectory else videosDirectory
      val goldenDir = if (fps == -1) goldenImagesDirectory else goldenVideosDirectory
      val hashes = mutableListOf<String>()
      val file = File(snapshotDir, snapshot.toFileName(extension = "temp.png"))
      val writer = ApngWriter(file.path.toPath(), fps)

      override fun handle(image: BufferedImage) {
        hashes += hash(image)
        writer.writeImage(image)
      }

      override fun close() {
        if (hashes.isEmpty()) return
        writer.close()
        val hashedFile = File(snapshotDir, "${hash(hashes)}.png")
        file.renameTo(hashedFile)
        file.delete()

        val goldenFile = File(goldenDir, snapshot.toFileName("_", "png"))
        hashedFile.copyTo(target = goldenFile, overwrite = true)
      }
    }
  }

  /** Returns a SHA-1 hash of the pixels of [image]. */
  private fun hash(image: BufferedImage): String {
    val hashingSink = HashingSink.sha1(blackholeSink())
    hashingSink.buffer().use { sink ->
      for (y in 0 until image.height) {
        for (x in 0 until image.width) {
          sink.writeInt(image.getRGB(x, y))
        }
      }
    }
    return hashingSink.hash.hex()
  }

  /** Returns a SHA-1 hash of [lines]. */
  private fun hash(lines: List<String>): String {
    val hashingSink = HashingSink.sha1(blackholeSink())
    hashingSink.buffer().use { sink ->
      for (hash in lines) {
        sink.writeUtf8(hash)
        sink.writeUtf8("\n")
      }
    }
    return hashingSink.hash.hex()
  }

  /** Release all resources and block until everything has been written to the file system. */
  override fun close() {
  }
}
