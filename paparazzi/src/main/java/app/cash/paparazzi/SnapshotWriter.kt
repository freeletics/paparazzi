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
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

public class SnapshotWriter @JvmOverloads constructor(
  rootDirectory: File = File(System.getProperty("paparazzi.report.dir"))
) : SnapshotHandler {
  private val imagesDirectory: File = File(rootDirectory, "images")
  private val goldenImagesDirectory: File = File(rootDirectory, "goldenImages")

  init {
    imagesDirectory.mkdirs()
    goldenImagesDirectory.mkdirs()
  }

  override fun newFrameHandler(
    snapshot: Snapshot,
    frameCount: Int,
    fps: Int
  ): FrameHandler {
    return object : FrameHandler {
      val hashes = mutableListOf<String>()

      override fun handle(image: BufferedImage) {
        hashes += writeImage(image)
      }

      override fun close() {
        if (hashes.isEmpty()) return

        if (hashes.size == 1) {
          val original = File(imagesDirectory, "${hashes[0]}.png")
          val goldenFile = File(goldenImagesDirectory, snapshot.toFileName("_", "png"))
          original.copyTo(goldenFile, overwrite = true)
        } else {
          // does not handle videos at the moment since SnapshotVerifier also does not support them
        }
      }
    }
  }

  /** Returns the hash of the image. */
  private fun writeImage(image: BufferedImage): String {
    val hash = hash(image)
    val file = File(imagesDirectory, "$hash.png")
    if (!file.exists()) {
      file.writeAtomically(image)
    }
    return hash
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

  /** Release all resources and block until everything has been written to the file system. */
  override fun close() {
  }

  private fun File.writeAtomically(bufferedImage: BufferedImage) {
    val tmpFile = File(parentFile, "$name.tmp")
    ImageIO.write(bufferedImage, "PNG", tmpFile)
    delete()
    tmpFile.renameTo(this)
  }
}
