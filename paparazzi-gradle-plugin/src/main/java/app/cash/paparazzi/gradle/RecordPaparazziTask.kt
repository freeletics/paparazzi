package app.cash.paparazzi.gradle

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class RecordPaparazziTask : PaparazziPlugin.PaparazziTask() {

  @get:Internal
  internal abstract val reportsDirectory: DirectoryProperty

  @InputDirectory
  internal fun getImageSnapshots(): Provider<Directory> {
    return reportsDirectory.dir("goldenImages")
  }

  // Don't declare as output so that Gradle doesn't require
  // a depnendency from test task to this task.
  // Since this is just copying files it's ok to run it every time.
  @get:Internal
  internal abstract val goldenSnapshotDirectory: DirectoryProperty

  @TaskAction
  public fun copy() {
    val goldenImagesDirectory = goldenSnapshotDirectory.dir("images").get()
    getImageSnapshots().get().asFile.listFiles()!!.forEach { snapshotFile ->
      val target = goldenImagesDirectory.file(snapshotFile.name)
      snapshotFile.copyTo(target.asFile, overwrite = true)
    }
  }
}
