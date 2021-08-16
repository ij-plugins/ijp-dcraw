// @formatter:off
import java.net.URL

name         := "ijp_dcraw"
organization := "net.sf.ij-plugins"
version      := "1.6.0.1-SNAPSHOT"

homepage             := Some(new URL("https://github.com/ij-plugins/ijp-dcraw"))
organizationHomepage := Some(url("https://github.com/ij-plugins"))
startYear            := Some(2008)
licenses             := Seq(("LGPL-2.1", new URL("http://opensource.org/licenses/LGPL-2.1")))
description          := "ijp-dcraw provides ImageJ plugin to read raw images using DCRAW. " +
  "For more detailed information see IJP-ImageIO home page: https://github.com/ij-plugins/ijp-dcraw."

libraryDependencies ++= Seq(
  "net.imagej"   % "ij"              % "1.53j",
  "junit"        % "junit"           % "4.13.2"  % "test",
  "com.novocode" % "junit-interface" % "0.11"    % "test->default"
  )

// fork a new JVM for 'run' and 'test:run'
fork := true

// add a JVM option to use when forking a JVM for 'run'
javaOptions ++= Seq("-Xmx2G", "-server")
Compile/compile/javacOptions ++= Seq(
  "-Xlint",
  "-target", "1.8",
  "-source", "1.8"
  )
Compile/doc/javacOptions ++= Seq(
  "-windowtitle", "IJP-DCRaw API v." + version.value,
  "-header",      "IJP-DCRaw API v." + version.value,
  "-sourcepath",  (baseDirectory.value / "src/main/java").getAbsolutePath,
  "-verbose"
  )

lazy val prepareRun = taskKey[Unit]("Prepare Run.")
prepareRun := {
  // Prepare sandbox
  val sandboxDir = baseDirectory.value / "sandbox"
  if (!sandboxDir.exists()) sandboxDir.mkdirs()

  val dcrawDstDir = sandboxDir / "plugins" / "dcraw"
  if (!dcrawDstDir.exists()) dcrawDstDir.mkdirs()

  val dcrawFiles = Seq(
    baseDirectory.value / "binaries" / "windows" / "dcraw_emu.exe",
    baseDirectory.value / "binaries" / "windows" / "libraw.dll",
    baseDirectory.value / "binaries" / "macOS"   / "dcraw_emu"
    )

  dcrawFiles.foreach{f => sbt.IO.copyFile(f, dcrawDstDir / f.name)}
}
// Make sure that `prepareRun` runs when needed
Compile / run := (Compile / run).dependsOn(prepareRun).evaluated
(Test / test) := (Test / test).dependsOn(prepareRun).value
(Test / testQuick) := (Test / testQuick).dependsOn(prepareRun).evaluated
ijPrepareRun := ijPrepareRun.value ++ {
  prepareRun.value
  Seq.empty[java.io.File]
}

//
// Setup sbt-imagej plugins
//
enablePlugins(SbtImageJ)
ijRuntimeSubDir         := "sandbox"
ijPluginsSubDir         := "ij-plugins"
ijCleanBeforePrepareRun := true
cleanFiles              += ijPluginsDir.value

run/baseDirectory := baseDirectory.value / "sandbox"
Test/baseDirectory := baseDirectory.value / "sandbox"

//
// Customize Java style publishing
//
// Enables publishing to maven repo
publishMavenStyle := true
// This is a Java project, disable using the Scala version in output paths and artifacts
crossPaths        := false
// This forbids including Scala related libraries into the dependency
autoScalaLibrary  := false
publishTo         := sonatypePublishToBundle.value
import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("ij-plugins", "ijp-dcraw", "jpsacha@gmail.com"))
developers := List(
  Developer(id="jpsacha", name="Jarek Sacha", email="jpsacha@gmail.com", url=url("https://github.com/jpsacha"))
  )

Test/packageBin/publishArtifact := false
Test/packageDoc/publishArtifact := false
Test/packageSrc/publishArtifact := false
