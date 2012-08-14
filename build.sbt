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

version := "0.6.2"

sbtPlugin := true

organization := "org.clapper"

licenses := Seq("BSD-like" ->
  url("http://software.clapper.org/sbt-editsource/license.html")
)

description := "SBT plugin to edit files on the fly"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked")

scalaVersion := "2.9.2"

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sed", "edit", "filter")

(description in LsKeys.lsync) <<= description(d => d)

// ---------------------------------------------------------------------------
// Other dependendencies

// External deps
libraryDependencies ++= Seq(
    "org.clapper" % "grizzled-scala_2.9.1" % "1.0.12"
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

publishMavenStyle := false

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false
