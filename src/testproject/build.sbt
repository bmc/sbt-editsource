
name := "sbt-editsource-test"

version := "0.2"

organization := "org.clapper"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.10"

seq(EditSource.settings: _*)

(sources in EditSource.Config) <++= baseDirectory map { d =>
    (d / "src" * "*.txt").get ++
    (d / "src" * "*.md").get
}

//sourceFiles in EditSource <++= 

EditSource.targetDirectory <<= baseDirectory(_ / "target")

EditSource.variables in EditSource.Config <+= organization {org => ("organization", org)}

EditSource.variables in EditSource.Config += ("foo", "bar")

EditSource.flatten  := false

EditSource.substitutions in EditSource.Config += sub("""build""".r, "Build")

EditSource.substitutions in EditSource.Config ++= Seq(
    sub("""(?i)\btest\b""".r, "TEST", SubAll),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
)

