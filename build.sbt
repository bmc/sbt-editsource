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

version := "0.6"

sbtPlugin := true

organization := "org.clapper"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked")

crossScalaVersions := Seq("2.8.1", "2.9.0", "2.9.0-1", "2.9.1")

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sed", "edit", "filter")

(description in LsKeys.lsync) := "SBT plugin to edit files on the fly"

// ---------------------------------------------------------------------------
// Other dependendencies

// External deps
libraryDependencies ++= Seq(
    "org.clapper" %% "grizzled-scala" % "1.0.10"
)

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
                       ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                     else
                       ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false
