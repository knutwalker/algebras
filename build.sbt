import com.typesafe.sbt.pgp.PgpKeys._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._
import de.heikoseeberger.sbtheader.license._


lazy val effect = project enablePlugins AutomateHeaderPlugin settings (
  algebrasSettings,
  name := "algebra-effect",
  libraryDependencies ++= List(
    "org.scalaz" %% "scalaz-core" % scalazVersion.value
      exclude("org.scala-lang", "scala-library")
      exclude("org.scala-lang.modules", s"scala-parser-combinators_${scalaCrossVersion.value}")
      exclude("org.scala-lang.modules", s"scala-xml_${scalaCrossVersion.value}"),
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.3"))

lazy val log = project enablePlugins AutomateHeaderPlugin dependsOn effect settings (
  algebrasSettings,
  name := "algebra-log")

lazy val random = project enablePlugins AutomateHeaderPlugin dependsOn effect settings (
  algebrasSettings,
  name := "algebra-random")

lazy val rng = project in file("interpreters") / "rng" dependsOn random enablePlugins AutomateHeaderPlugin settings (
  algebrasSettings,
  name := "algebra-interpreter-rng",
  libraryDependencies ++= List(
    "org.scalaz" %% "scalaz-effect" % scalazVersion.value
      exclude("org.scalaz", s"scalaz-core_${scalaCrossVersion.value}"),
    "com.nicta"  %% "rng"           % "1.3.0"
      exclude("org.scalaz", s"scalaz-effect_${scalaCrossVersion.value}")
      exclude("org.scalaz", s"scalaz-core_${scalaCrossVersion.value}")))

lazy val slf4j = project in file("interpreters") / "slf4j" dependsOn log enablePlugins AutomateHeaderPlugin settings (
  algebrasSettings,
  name := "algebra-interpreter-slf4j",
  libraryDependencies ++= List(
    "org.scalaz" %% "scalaz-effect" % scalazVersion.value
      exclude("org.scalaz", s"scalaz-core_${scalaCrossVersion.value}"),
    "org.slf4j"   % "slf4j-api"     % "1.7.12"))

lazy val parent = project in file(".") dependsOn (effect, log, random) aggregate (effect, log, random, rng, slf4j) enablePlugins AutomateHeaderPlugin settings (
  algebrasSettings,
  name := "algebra-parent")

// =================================

lazy val buildSettings = List(
        organization := "de.knutwalker",
        scalaVersion := "2.11.6",
       scalazVersion := "7.1.1",
  crossScalaVersions := "2.11.6" :: Nil)

lazy val commonSettings = List(
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-deprecation",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-Xcheckinit",
    "-Xfatal-warnings",
    "-Xfuture",
    "-Xlint",
    "-Yclosure-elim",
    "-Ydead-code",
    "-Yno-adapted-args",
    "-Yno-predef",
    "-Ywarn-adapted-args",
    "-Ywarn-inaccessible",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit"),
  scalacOptions in Test += "-Yrangepos",
  scalacOptions in (Compile, console) ~= (_ filterNot (x ⇒ x == "-Xfatal-warnings" || x.startsWith("-Ywarn"))),
  scalaCrossVersion := scalaVersion.value.substring(0, 4),
  shellPrompt := { state ⇒
    val name = Project.extract(state).currentRef.project
    (if (name == "parent") "" else name + " ") + "> "
  },
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  headers <<= (maintainer, startYear) { (m, y) ⇒
    val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = List(y.getOrElse(thisYear), thisYear).distinct.mkString(" – ")
    Map("scala" -> Apache2_0(years, m))
  },
  initialCommands in      console := """import scalaz._, Scalaz._, algebras._, Algebras._""",
  initialCommands in consoleQuick := """import scalaz._, Scalaz._""",
  logBuffered := false)


lazy val publishSettings = releaseSettings ++ sonatypeSettings ++ List(
                 startYear := Some(2015),
         publishMavenStyle := true,
   publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
                maintainer := "Paul Horn",
  SonatypeKeys.profileName := "knutwalker",
                githubUser := "knutwalker",
                githubRepo := "algebras",
                  homepage := Some(url(s"https://github.com/${githubUser.value}/${githubRepo.value}")),
                  licenses := List("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
                   scmInfo := _scmInfo(githubUser.value, githubRepo.value),
               tagComment <<= version map (v => s"Release version $v"),
            commitMessage <<= version map (v => s"Set version to $v"),
               versionBump := sbtrelease.Version.Bump.Bugfix,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := {
    <developers>
      <developer>
        <id>${githubUser.value}</id>
        <name>${maintainer.value}</name>
        <url>http://knutwalker.de/</url>
      </developer>
    </developers>
  },
  releaseProcess := List[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishSignedArtifacts,
    releaseToCentral,
    setNextVersion,
    commitNextVersion,
    pushChanges,
    publishArtifacts
  )
)

lazy val publishSignedArtifacts = publishArtifacts.copy(
  action = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(publishSigned in Global in ref, st)
  },
  enableCrossBuild = true
)

lazy val releaseToCentral = ReleaseStep(
  action = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(sonatypeReleaseAll in Global in ref, st)
  },
  enableCrossBuild = true
)

lazy val algebrasSettings = buildSettings ++ commonSettings ++ publishSettings

lazy val scalazVersion = SettingKey[String]("Scalaz version")
lazy val scalaCrossVersion = SettingKey[String]("Scala cross version prefix")

lazy val maintainer = SettingKey[String]("Maintainer")
lazy val githubUser = SettingKey[String]("Github username")
lazy val githubRepo = SettingKey[String]("Github repository")

def _scmInfo(user: String, repo: String) = Some(ScmInfo(
  url(s"https://github.com/$user/$repo"),
  s"scm:git:https://github.com/$user/$repo.git",
  Some(s"scm:git:ssh://git@github.com:$user/$repo.git")
))
