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

    lazy val home = Path.fileProperty("user.home")
    lazy val publishTo = Resolver.sftp("clapper.org Maven Repo",
                                       "maven.clapper.org",
                                       "/var/www/maven.clapper.org/html") as
                         ("bmc", (home / ".ssh" / "id_dsa").asFile)

    override def managedStyle = ManagedStyle.Maven

}
