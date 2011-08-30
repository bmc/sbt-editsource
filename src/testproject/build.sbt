
name := "sbt-editsource-test"

version := "0.1"

organization := "org.clapper"

scalaVersion := "2.8.1"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.7"

seq(org.clapper.sbt.editsource.EditSource.editSourceSettings: _*)

EditSource.sourceFiles in EditSource.Config <++= baseDirectory { d =>
    (d / "src" * "*.txt").get ++
    (d / "src" * "*.md").get
}

//sourceFiles in EditSource <++= 

EditSource.targetDirectory in EditSource.Config <<= baseDirectory(_ / "target")

EditSource.variables in EditSource.Config <+= organization {org =>
  ("organization", org)
}

EditSource.variables in EditSource.Config += ("foo", "bar")

logLevel := Level.Debug

EditSource.flatten in EditSource.Config := false

EditSource.substitutions in EditSource.Config += sub("""build""".r, "Build")

EditSource.substitutions in EditSource.Config ++= Seq(
    sub("""(?i)\btest\b""".r, "TEST", SubAll),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
)

