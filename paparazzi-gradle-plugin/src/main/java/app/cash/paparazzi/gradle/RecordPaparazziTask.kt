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
  internal fun getGoldenSnapshotSource(): Provider<Directory> {
    return reportsDirectory.dir("golden")
  }

  // Don't declare as output so that Gradle doesn't require
  // a dependency from test task to this task.
  // Since this is just copying files it's ok to run it every time.
  @get:Internal
  internal abstract val goldenSnapshotTargetDirectory: DirectoryProperty

  @TaskAction
  public fun copy() {
    val source = getGoldenSnapshotSource().get().asFile
    val target = goldenSnapshotTargetDirectory.get().asFile
    target.deleteRecursively()
    target.mkdirs()
    source.copyRecursively(target, overwrite = true)
  }
}
