// ---------------------------------------------------------------------------
// SBT Build File for SBT EditSource Plugin
//
// Copyright (c) 2010-2015 Brian M. Clapper
//
// See accompanying license file for license information.
// ---------------------------------------------------------------------------

import bintray.Keys._

// ---------------------------------------------------------------------------
// Basic settings

name := "sbt-editsource"

version := "0.7.0"

sbtPlugin := true

organization := "org.clapper"

licenses += ("BSD New", url("https://github.com/bmc/sbt-editsource/blob/master/LICENSE.md"))

description := "SBT plugin to edit files on the fly"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

crossScalaVersions := Seq("2.10.4")

seq(lsSettings: _*)

(LsKeys.tags in LsKeys.lsync) := Seq("sed", "edit", "filter")

(description in LsKeys.lsync) <<= description(d => d)

// ---------------------------------------------------------------------------
// Other dependendencies

// External deps
libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.3"

// ---------------------------------------------------------------------------
// Publishing criteria

publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

