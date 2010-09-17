/**
 * Parent project for SBT Plugins.
 */

import sbt._

class SBTPluginsProject(info: ProjectInfo)
    extends ParentProject(info)
    with posterous.Publish
{
    /* ---------------------------------------------------------------------- *\
                                Subprojects
    \* ---------------------------------------------------------------------- */

    lazy val izPack = project("IzPack")
    lazy val markdown = project("markdown")
    lazy val editsource = project("editsource")

    lazy val publishTo = "Scala Tools Nexus" at
        "http://nexus.scala-tools.org/content/repositories/releases/"
    Credentials(Path.userHome / "src" / "mystuff" / "scala" /
                "nexus.scala-tools.org.properties", log)

    override def managedStyle = ManagedStyle.Maven
}
