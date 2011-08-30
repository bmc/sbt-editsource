// ---------------------------------------------------------------------------
// SBT 0.10.x Build File for SBT EditSource Plugin
//
// Copyright (c) 2010-2011 Brian M. Clapper
//
// See accompanying license file for license information.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Basic settings

name := "sbt-editsource"

version := "0.4.1"

sbtPlugin := true

organization := "org.clapper"

scalaVersion := "2.8.1"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked")

// ---------------------------------------------------------------------------
// Posterous-SBT

libraryDependencies <<= (sbtVersion, scalaVersion, libraryDependencies) { (sbtv, scalav, deps) =>
    if (scalav == "2.8.1")
        deps :+ "net.databinder" %% "posterous-sbt" % ("0.3.0_sbt" + sbtv)
    else
        deps
}

(name in Posterous) := "sbt-editsource"

// ---------------------------------------------------------------------------
// Other dependendencies

// External deps
libraryDependencies ++= Seq(
    "org.clapper" %% "grizzled-scala" % "1.0.7",
    "org.scala-lang" % "scala-compiler" % "2.8.1"
)

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo <<= version {(v: String) =>
    val nexus = "http://nexus.scala-tools.org/content/repositories/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
    else                             Some("releases"  at nexus + "releases/")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / "src" / "mystuff" / "scala" /
                           "nexus.scala-tools.org.properties")

