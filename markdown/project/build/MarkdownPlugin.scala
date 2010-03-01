import sbt._

class MarkdownPluginProject(info: ProjectInfo) extends PluginProject(info)
{
    /* ---------------------------------------------------------------------- *\
                                Publishing
    \* ---------------------------------------------------------------------- */

    // "publish" will prompt (via a Swing pop-up) for the username and
    // password.
    val publishTo = Resolver.sftp("clapper.org Maven Repo",
                                  "maven.clapper.org",
                                  "/var/www/maven.clapper.org/html")

    override def managedStyle = ManagedStyle.Maven

    /* ---------------------------------------------------------------------- *\
                           Managed Dependencies
    \* ---------------------------------------------------------------------- */

    val rhino = "rhino" % "js" % "1.7R2"
}
