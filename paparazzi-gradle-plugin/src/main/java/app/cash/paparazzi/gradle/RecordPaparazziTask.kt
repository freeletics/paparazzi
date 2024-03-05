package app.cash.paparazzi.gradle

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.Locale

public abstract class RecordPaparazziTask : PaparazziPlugin.PaparazziTask() {

  @get:Internal
  internal abstract val reportsDirectory: DirectoryProperty

  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  internal fun getSnapshotsJson(): Provider<RegularFile> {
    return reportsDirectory.file("snapshots.json")
  }

  @InputDirectory
  internal fun getImageSnapshots(): Provider<Directory> {
    return reportsDirectory.dir("images")
  }

  @InputDirectory
  internal fun getVideoSnapshots(): Provider<Directory> {
    return reportsDirectory.dir("videos")
  }

  // Don't declare as output so that Gradle doesn't require
  // a depnendency from test task to this task.
  // Since this is just copying files it's ok to run it every time.
  @get:Internal
  internal abstract val goldenSnapshotDirectory: DirectoryProperty

  @TaskAction
  public fun copy() {
    val content = getSnapshotsJson()
      .get()
      .asFile
      .source()
      .buffer()
    val snapshots = PaparazziJson.listOfShotsAdapter.fromJson(content)
    val reportsDirectory = reportsDirectory.get().asFile
    snapshots!!.forEach { snapshot ->
      val snapshotFile = reportsDirectory.resolve(snapshot.file)
      val targetFileName = snapshot.toFileName(delimiter = "_", snapshotFile.extension)
      val target = goldenSnapshotDirectory.dir(snapshotFile.parentFile.name).get().file(targetFileName)
      snapshotFile.copyTo(target.asFile, overwrite = true)
    }
  }

  private class Snapshot(
    val name: String?,
    val testName: TestName,
    val file: String
  )

  private class TestName(
    val packageName: String,
    val className: String,
    val methodName: String
  )

  private object PaparazziJson {
    val moshi = Moshi.Builder()
      .add(this)
      .addLast(KotlinJsonAdapterFactory())
      .build()!!

    val listOfShotsAdapter: JsonAdapter<List<Snapshot>> =
      moshi
        .adapter<List<Snapshot>>(
          Types.newParameterizedType(List::class.java, Snapshot::class.java)
        )
        .indent("  ")

    @ToJson
    fun testNameToJson(testName: TestName): String {
      return "${testName.packageName}.${testName.className}#${testName.methodName}"
    }

    @FromJson
    fun testNameFromJson(json: String): TestName {
      val regex = Regex("(.*)\\.([^.]*)#([^.]*)")
      val (packageName, className, methodName) = regex.matchEntire(json)!!.destructured
      return TestName(packageName, className, methodName)
    }
  }

  private fun Snapshot.toFileName(
    delimiter: String = "_",
    extension: String
  ): String {
    val formattedLabel = if (name != null) {
      "$delimiter${name.toLowerCase(Locale.US).replace("\\s".toRegex(), delimiter)}"
    } else {
      ""
    }
    return "${testName.packageName}${delimiter}${testName.className}${delimiter}${testName.methodName}$formattedLabel.$extension"
  }
}
