// ---------------------------------------------------------------------------
// SBT Build File for SBT EditSource Plugin
//
// Copyright (c) 2010-2015 Brian M. Clapper
//
// See accompanying license file for license information.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Basic settings

name := "sbt-editsource"

version := "0.8.0"

sbtPlugin := true

organization := "org.clapper"

licenses += ("BSD New", url("https://github.com/bmc/sbt-editsource/blob/master/LICENSE.md"))

description := "SBT plugin to edit files on the fly"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

crossScalaVersions := Seq("2.10.6")

// ---------------------------------------------------------------------------
// Other dependendencies

// External deps
libraryDependencies += "org.clapper" %% "grizzled-scala" % "4.2.0"

// ---------------------------------------------------------------------------
// Publishing criteria

publishMavenStyle := false

// ---------------------------------------------------------------------------
// Publishing criteria

// Don't set publishTo. The Bintray plugin does that automatically.

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
pomExtra :=
  <scm>
    <url>git@github.com:bmc/grizzled-scala.git/</url>
    <connection>scm:git:git@github.com:bmc/sbt-editsource.git</connection>
  </scm>
    <developers>
      <developer>
        <id>bmc</id>
        <name>Brian Clapper</name>
        <url>http://www.clapper.org/bmc</url>
      </developer>
    </developers>

