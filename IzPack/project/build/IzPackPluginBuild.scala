import sbt._

class IzPackPluginProject(info: ProjectInfo) extends PluginProject(info)
{
    /* ---------------------------------------------------------------------- *\
                               Dependencies
    \* ---------------------------------------------------------------------- */

    val izPack = "org.codehaus.izpack" % "izpack-standalone-compiler" % "4.3.2"

    /* ---------------------------------------------------------------------- *\
                                Publishing
    \* ---------------------------------------------------------------------- */

    // "publish" will prompt (via a Swing pop-up) for the username and
    // password.
    val publishTo = Resolver.sftp("clapper.org Maven Repo",
                                  "maven.clapper.org",
                                  "/var/www/maven.clapper.org/html")

    override def managedStyle = ManagedStyle.Maven
}
