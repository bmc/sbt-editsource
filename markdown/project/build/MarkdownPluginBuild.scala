import sbt._

class MarkdownPluginProject(info: ProjectInfo) extends PluginProject(info)
{
    /* ---------------------------------------------------------------------- *\
                                Publishing
    \* ---------------------------------------------------------------------- */

    // Enable publishing in this subproject, with no dependencies on other
    // subprojects. For more details, see the relevent SBT wiki section:
    // http://code.google.com/p/simple-build-tool/wiki/SubProjects

    override def deliverProjectDependencies = Nil

    /* ---------------------------------------------------------------------- *\
                           Managed Dependencies
    \* ---------------------------------------------------------------------- */

    val rhino = "rhino" % "js" % "1.7R2"
}
