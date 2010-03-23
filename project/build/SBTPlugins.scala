/**
 * Parent project for SBT Plugins.
 */

import sbt._

class SBTPluginsProject(info: ProjectInfo) extends ParentProject(info)
{
    /* ---------------------------------------------------------------------- *\
                                Subprojects
    \* ---------------------------------------------------------------------- */

    lazy val izPack = project("IzPack")
    lazy val markdown = project("markdown")
    lazy val editsource = project("editsource")

    // "publish" will prompt (via a Swing pop-up) for the username and
    // password.
    val publishTo = Resolver.sftp("clapper.org Maven Repo",
                                  "maven.clapper.org",
                                  "/var/www/maven.clapper.org/html")

    override def managedStyle = ManagedStyle.Maven

}
