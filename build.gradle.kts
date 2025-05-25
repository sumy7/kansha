import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.jvm.optionals.getOrNull

val libFinder = versionCatalogs.find("libs").get()
var REAL_VERSION = libs.versions.libraryVersion.get()
val JVM_TARGET = JvmTarget.JVM_17
val JDK_VERSION = JavaVersion.VERSION_17

val GROUP = "com.sumygg"

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
    version = REAL_VERSION
    group = GROUP
}