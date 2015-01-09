
name := "sbt-editsource-test"

version := "0.3"

organization := "org.clapper"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.10"

(sources in EditSource) <++= baseDirectory map { d =>
    (d / "src" * "*.txt").get ++
    (d / "src" * "*.md").get
}

targetDirectory in EditSource <<= baseDirectory(_ / "target")

variables in EditSource <+= organization {org => ("organization", org)}

variables in EditSource += ("foo", "bar")

flatten in EditSource := false

substitutions in EditSource += sub("""build""".r, "Build")

substitutions in EditSource ++= Seq(
    sub("""(?i)\btest\b""".r, "TEST", SubAll),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
)

