
name := "sbt-editsource-test"

version := "0.4"

organization := "org.clapper"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "4.4.2"

(sources in EditSource) ++= (baseDirectory.value / "src" * "*.txt").get ++
                            (baseDirectory.value / "src" * "*.md").get

//    (bd / "src" * "*.md")

targetDirectory in EditSource := baseDirectory.value / "target"

variables in EditSource += "organization" -> organization.value

variables in EditSource += "foo" -> "bar"

flatten in EditSource := true

substitutions in EditSource += sub("""build""".r, "Build")

substitutions in EditSource ++= Seq(
    sub("""(?i)\btest\b""".r, "TEST", SubAll),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
)

compile in Compile := ((compile in Compile) dependsOn (edit in EditSource)).value
