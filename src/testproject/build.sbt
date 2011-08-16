
name := "sbt-editsource-test"

version := "0.1"

organization := "org.clapper"

scalaVersion := "2.8.1"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.7"

seq(org.clapper.sbt.editsource.EditSource.editSourceSettings: _*)

sourceFiles in EditSource <++= baseDirectory(d => (d / "src" * "*.txt").get)

targetDirectory in EditSource <<= baseDirectory(_ / "target")

variables in EditSource <+= organization {org => ("organization", org)}

variables in EditSource += ("foo", "bar")

substitutions in EditSource := Seq(("""\b[Tt]est\b""".r, "TEST", SubAll))
