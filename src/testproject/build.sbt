
name := "sbt-editsource-test"

version := "0.1"

organization := "org.clapper"

scalaVersion := "2.8.1"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.7"

seq(org.clapper.sbt.editsource.EditSource.editSourceSettings: _*)

EditSource.sources <++= baseDirectory { d =>
    (d / "src" * "*.txt").get ++
    (d / "src" * "*.md").get
}

//sourceFiles in EditSource <++= 

EditSource.targetDirectory <<= baseDirectory(_ / "target")

EditSource.variables <+= organization {org => ("organization", org)}

EditSource.variables += ("foo", "bar")

EditSource.logLevel := Level.Debug

EditSource.flatten  := false

EditSource.substitutions += sub("""build""".r, "Build")

EditSource.substitutions ++= Seq(
    sub("""(?i)\btest\b""".r, "TEST", SubAll),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
)

